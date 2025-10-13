package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.clients.ProductClient;
import com.github.gavro081.orderservice.clients.UserClient;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.exceptions.OrderNotFoundException;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final NotificationService notificationService;
    private final ProductClient productClient;
    private final UserClient userClient;

    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        NotificationService notificationService,
                        ProductClient productClient,
                        UserClient userClient) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.notificationService = notificationService;
        this.productClient = productClient;
        this.userClient = userClient;
    }

    private Order getOrderById(UUID orderId) throws OrderNotFoundException{
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order " + orderId + "is not found"));
    }

    public List<Order> getOrders(){
        return orderRepository.findAll();
    }

    public UUID createOrder(OrderRequest orderRequest){
        UserDetailDto userDto = userClient.getUserByUsername(orderRequest.username());
        ProductDetailDto productDto = productClient.getProductByName(orderRequest.productName());

        Order newOrder = Order.builder()
                .userId(userDto.id())
                .productId(productDto.id())
                .quantity(Integer.parseInt(orderRequest.quantity()))
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(newOrder);
        logger.info("created order with id {}", savedOrder.getId());
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getProductId().toString(),
                savedOrder.getUserId().toString(),
                savedOrder.getQuantity(),
                orderRequest.username()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "order.created", event);
        return savedOrder.getId();
    }

    public void updateOrderStatus(String username, UUID orderId, OrderStatus newStatus) {
        Order order;
        try {
             order = getOrderById(orderId);
        } catch (OrderNotFoundException e) {
            logger.warn("Order {} not found.", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(newStatus);
            order.setTimestamp(Instant.now());
            orderRepository.save(order);
            logger.info("Order {} status updated to {}", orderId, newStatus);
            notificationService.notifyOrderStatusUpdate(username, orderId, newStatus);
        } else {
            logger.warn("Order {} already in a final state ({}). Ignoring status update to {}",
                    orderId, order.getStatus(), newStatus);
        }
    }

    public Order getLastOrder() {
        return orderRepository.findTopByTimestampIsNotNullOrderByTimestampDesc();
    }
}
