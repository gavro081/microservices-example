package com.github.gavro081.userservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.DebitFailureReason;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
import com.github.gavro081.common.events.BalanceDebitedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository, RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<User> getUsers(){
        return userRepository.findAll();
    }

    public User getUserById(Long userId){
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User " + userId + "is not found"));
    }

    public void debitUserBalance(InventoryReservedEvent event) {
        User user;
        try {
            user = getUserById(Long.parseLong(event.getUserId()));
        } catch (RuntimeException e){
            logger.warn("Could not bill user because user {} is not found. Publishing failure event.", event.getUserId());
            publishFailureEvent(
                    event,
                    DebitFailureReason.USER_NOT_FOUND,
                    "User not found"
            );
            return;
        }
        if (event.getTotalPrice() <= user.getBalance()){
            user.setBalance(user.getBalance() - event.getTotalPrice());
            userRepository.save(user);
            publishSuccessEvent(event, user);
        } else {
            logger.warn("Insufficient funds, needed: {}, user has: {}", event.getTotalPrice(), user.getBalance());
            publishFailureEvent(
                    event,
                    DebitFailureReason.INSUFFICIENT_FUNDS,
                    "Insufficient funds"
            );
        }

    }

    private void publishFailureEvent(InventoryReservedEvent event, DebitFailureReason reason, String message) {
        //todo: replace with builder
        BalanceDebitFailedEvent failedEvent = new BalanceDebitFailedEvent(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                reason,
                message
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "balance.failed", failedEvent);
    }
    private void publishSuccessEvent(InventoryReservedEvent event, User user){
        // todo: replace with builder
        BalanceDebitedEvent newEvent = new BalanceDebitedEvent(
                event.getOrderId(),
                user.getId(),
                event.getProductId(),
                event.getTotalPrice(),
                event.getProductName()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "balance.success", newEvent);
    }
}
