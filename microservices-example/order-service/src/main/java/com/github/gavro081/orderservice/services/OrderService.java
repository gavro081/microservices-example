package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final WebClient.Builder webClientBuilder;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        WebClient.Builder webClientBuilder,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.webClientBuilder = webClientBuilder;
        this.notificationService = notificationService;
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

    public UUID createOrder(OrderRequest orderRequest){
        UserDetailDto userDto = getUserIdFromUsername(orderRequest.username());
        ProductDetailDto productDto = getProductIdFromProductName(orderRequest.productName());

        if (userDto == null || productDto == null) {
            // todo: add better error handling
            logger.error("User or product not found, cannot create order.");
            return null;
        }

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
        } catch (RuntimeException e) {
            logger.warn("Order {} not found.", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(newStatus);
            orderRepository.save(order);
            logger.info("Order {} status updated to {}", orderId, newStatus);
            notificationService.notifyOrderStatusUpdate(username, orderId, newStatus);
        } else {
            logger.warn("Order {} already in a final state ({}). Ignoring status update to {}",
                    orderId, order.getStatus(), newStatus);
        }
    }

    private ProductDetailDto getProductIdFromProductName(String productName) {
        // todo: service discovery ? or something else, but dont hardcode the uri
        return webClientBuilder.build().get()
                .uri("http://localhost:8081/products/by-name/{name}", productName)
                .retrieve()
                .bodyToMono(ProductDetailDto.class)
                .block();
    }

    private UserDetailDto getUserIdFromUsername(String username) {
        // todo: service discovery ? or something else, but dont hardcode the uri
        // read what exactly each of these do
        return webClientBuilder.build().get()
                .uri("http://localhost:8082/users/by-username/{username}", username)
                .retrieve()
                .bodyToMono(UserDetailDto.class)
                .block();
    }
}
