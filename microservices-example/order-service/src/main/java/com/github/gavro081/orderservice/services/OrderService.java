package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getProductId().toString(),
                savedOrder.getUserId().toString(),
                savedOrder.getQuantity()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "order.created", event);
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
