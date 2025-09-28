package com.github.gavro081.productservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.ReservationFailureReason;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
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

    private static final String CONTEXT_RESERVE = "INVENTORY_RESERVATION";
    private static final String CONTEXT_RELEASE = "INVENTORY_RELEASE";

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
            processedEventService.markActionAsProcessed(event.getEventId(), CONTEXT_RESERVE);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate INVENTORY_RELEASE for order {}, ignoring.", event.getOrderId());
            return;
        }

        Product product;
        try {
             product = getProductById(Long.parseLong(event.getProductId()));
        } catch (RuntimeException e) {
            logger.warn("Product not found for event {}. Publishing failure event.", event.getEventId());
            publishFailureEvent(event, ReservationFailureReason.PRODUCT_NOT_FOUND, "Product could not be found");
            return;
        }

        if (product.getQuantity() >= event.getQuantity()){
            product.setQuantity(product.getQuantity() - event.getQuantity());
            productRepository.save(product);
            publishSuccessEvent(event, product);
        } else {
            publishFailureEvent(
                    event,
                    ReservationFailureReason.INSUFFICIENT_STOCK,
                    "Insufficient stock, available items: " + product.getQuantity());
        }
    }

    @Transactional
    public void freeInventory(BalanceDebitFailedEvent failedEvent) {
        try {
            processedEventService.markActionAsProcessed(failedEvent.getEventId(), CONTEXT_RELEASE);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate INVENTORY_RESERVATION for order {}, ignoring.", failedEvent.getOrderId());
            return;
        }

        // todo: handle thrown error if product is not found
        Product product = getProductById(Long.parseLong(failedEvent.getProductId()));
        product.setQuantity(product.getQuantity() + failedEvent.getQuantity());
        productRepository.save(product);
        logger.info("Released {} items for product {} from order {}",
                failedEvent.getQuantity(), product.getId(), failedEvent.getOrderId());
    }

    private void publishFailureEvent(OrderCreatedEvent orderEvent, ReservationFailureReason reason, String message){
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
        // todo: replace with builder
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderEvent.getOrderId(),
                orderEvent.getUserId(),
                orderEvent.getProductId(),
                product.getName(),
                orderEvent.getQuantity(),
                product.getPrice(),
                product.getPrice() * orderEvent.getQuantity()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "inventory.reserved",
                reservedEvent);
    }

    public Product getProductByName(String name) {
        return productRepository.findByName(name);
    }
}
