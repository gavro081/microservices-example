package com.github.gavro081.orderservice.listeners;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
import com.github.gavro081.common.events.BalanceDebitedEvent;
import com.github.gavro081.common.events.InventoryReservationFailedEvent;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.services.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitHandler
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent failedEvent){
        log.info("Received InventoryReservationFailedEvent for orderId: {}, eventId: {}",
                failedEvent.getOrderId(), failedEvent.getEventId());
        orderService.updateOrderStatus(failedEvent.getUsername(), failedEvent.getOrderId(), OrderStatus.FAILED);
    }

    @RabbitHandler
    public void handleBalanceDebitedEvent(BalanceDebitedEvent event){
        log.info("Received BalancedDebitedEvent for orderId: {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        orderService.updateOrderStatus(event.getUsername(), event.getOrderId(), OrderStatus.COMPLETED);
    }

    @RabbitHandler
    public void handleBalanceDebitFailedEvent(BalanceDebitFailedEvent event){
        log.info("Received BalancedDebitedEventFailed for orderId: {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        orderService.updateOrderStatus(event.getUsername(),event.getOrderId(), OrderStatus.FAILED);
    }
}
