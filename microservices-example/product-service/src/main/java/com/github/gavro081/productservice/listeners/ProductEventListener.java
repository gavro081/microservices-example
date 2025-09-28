package com.github.gavro081.productservice.listeners;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.productservice.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {
    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);
    private final ProductService productService;

    public ProductEventListener(ProductService productService) {
        this.productService = productService;
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCTS_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent orderCreatedEvent){
        log.info("Received OrderCreatedEvent for orderId: {}", orderCreatedEvent.getOrderId());
        try {
            productService.reserveInventory(orderCreatedEvent);
        } catch (Exception e){
            log.info("Error occurred while processing orderId: {}", orderCreatedEvent.getOrderId());
        }
    }
}
