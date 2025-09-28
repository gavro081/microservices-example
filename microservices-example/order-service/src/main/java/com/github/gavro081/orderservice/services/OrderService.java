package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.FailureReason;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    private Order getOrderById(UUID orderId) {
        // todo: add custom exception
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order " + orderId + "is not found"));
    }

    public List<Order> getOrders(){
        return orderRepository.findAll();
    }

    public void createOrder(OrderRequest orderRequest){
        Long userId = getUserIdFromUsername(orderRequest.username());
        Long productId = getProductIdFromProductName(orderRequest.productName());
        Order newOrder = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(Integer.parseInt(orderRequest.quantity()))
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(newOrder);
        logger.info("created order with id {}", savedOrder.getId());
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getProductId().toString(),
                savedOrder.getUserId().toString(),
                savedOrder.getQuantity()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "order.created", event);
    }

    public void updateOrderStatus(UUID orderId, OrderStatus newStatus){
        Order order;
        try {
             order = getOrderById(orderId);
        } catch (RuntimeException e) {
            logger.warn("Order {} not found.", orderId);
            return;
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private Long getProductIdFromProductName(String productName) {
        // todo: implement a call to users service
        return 5L;
    }

    private Long getUserIdFromUsername(String username) {
        // todo: implement a call to products service
        return 1L;
    }
}
