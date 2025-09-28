package com.github.gavro081.orderservice.listeners;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.InventoryReservationFailedEvent;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.services.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent failedEvent){
        log.info("Received InventoryReservationFailedEvent for orderId: {}", failedEvent.getOrderId());
        orderService.updateOrderStatus(failedEvent.getOrderId(), OrderStatus.FAILED);
        // todo: send status failed back to client
    }
}
