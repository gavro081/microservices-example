package com.github.gavro081.userservice.listeners;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.userservice.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.USERS_QUEUE)
public class UserEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);
    private final UserService userService;

    public UserEventListener(UserService userService) {
        this.userService = userService;
    }

    @RabbitHandler
    public void handleInventoryReservedEvent(InventoryReservedEvent event){
        log.info("Received InventoryReservedEvent for orderId: {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        userService.debitUserBalance(event);
    }
}
