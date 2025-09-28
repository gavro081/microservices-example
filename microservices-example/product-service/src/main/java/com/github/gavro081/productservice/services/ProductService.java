package com.github.gavro081.productservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.FailureReason;
import com.github.gavro081.common.events.InventoryReservationFailedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProcessedEventService processedEventService;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository,
                          ProcessedEventService processedEventService,
                          RabbitTemplate rabbitTemplate) {
        this.productRepository = productRepository;
        this.processedEventService = processedEventService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Product> getProducts(){
        return productRepository.findAll();
    }

    public Product getProductById(Long id){
        // todo: add custom exception
        return productRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Product " + id + "is not found"));
    }

    @Transactional
    public void reserveInventory(OrderCreatedEvent event){
        try {
            processedEventService.markEventAsProcessed(event.getEventId());
        } catch (DataIntegrityViolationException e){
            logger.warn("Duplicate event received, ignoring. EventId: {}", event.getEventId());
            return;
        }

        Product product;
        try {
             product = getProductById(Long.parseLong(event.getProductId()));
        } catch (RuntimeException e) {
            logger.warn("Product not found for event {}. Publishing failure event.", event.getEventId());
            publishFailureEvent(event, FailureReason.PRODUCT_NOT_FOUND, "Product could not be found");
            return;
        }

        if (product.getQuantity() >= event.getQuantity()){
            product.setQuantity(product.getQuantity() - event.getQuantity());
            updateProduct(product);
            publishSuccessEvent(event, product);
        } else {
            publishFailureEvent(
                    event,
                    FailureReason.INSUFFICIENT_STOCK,
                    "Insufficient stock, available items: " + product.getQuantity());
        }
    }

    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    private void publishFailureEvent(OrderCreatedEvent orderEvent, FailureReason reason, String message){
        InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                orderEvent.getOrderId(),
                orderEvent.getProductId(),
                reason,
                message
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "inventory.failed",
                failedEvent);
    }

    private void publishSuccessEvent(OrderCreatedEvent orderEvent, Product product){
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderEvent.getOrderId(),
                orderEvent.getUserId(),
                orderEvent.getProductId(),
                orderEvent.getQuantity(),
                product.getPrice(),
                product.getPrice() * orderEvent.getQuantity()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "inventory.reserved",
                reservedEvent);
    }
}
