package com.github.gavro081.orderservice.services;

import com.github.gavro081.orderservice.models.OrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyOrderStatusUpdate(String username, UUID orderId, OrderStatus status){
        String destination = "/topic/order-status/" + username;

        Map<String, String> payload = Map.of(
                "username", username,
                "orderId", orderId.toString(),
                "status", status.toString()
        );

        messagingTemplate.convertAndSend(destination, payload);
    }
}
