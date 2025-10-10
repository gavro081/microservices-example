package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.exceptions.ExternalServiceException;
import com.github.gavro081.orderservice.exceptions.OrderNotFoundException;
import com.github.gavro081.orderservice.exceptions.ResourceNotFoundException;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final WebClient.Builder webClientBuilder;
    private final NotificationService notificationService;
    @Value("${microservices.product-service.url}")
    private String productServiceUrl;
    @Value("${microservices.user-service.url}")
    private String userServiceUrl;

    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        WebClient.Builder webClientBuilder,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.webClientBuilder = webClientBuilder;
        this.notificationService = notificationService;
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
        UserDetailDto userDto = getUserIdFromUsername(orderRequest.username());
        ProductDetailDto productDto = getProductIdFromProductName(orderRequest.productName());

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

    private ProductDetailDto getProductIdFromProductName(String productName) {
        String url = productServiceUrl + "/products/by-name/{name}";
        return webClientBuilder.build().get()
                .uri(url, productName)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> Mono.error(new ResourceNotFoundException("Product not found: " + productName)))
                .bodyToMono(ProductDetailDto.class)
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> {
                    throw new ExternalServiceException("Error fetching product details for " + productName, e);
                })
                .block();
    }

    private UserDetailDto getUserIdFromUsername(String username) {
        String url = userServiceUrl + "/users/by-username/{username}";
        return webClientBuilder.build().get()
                .uri(url, username)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> Mono.error(new ResourceNotFoundException("User not found: " + username)))
                .bodyToMono(UserDetailDto.class)
                .doOnError(e -> !(e instanceof ResourceNotFoundException), e -> {
                    throw new ExternalServiceException("Error fetching user details for " + username, e);
                })
                .block();
    }

    public Order getLastOrder() {
        return orderRepository.findTopByTimestampIsNotNullOrderByTimestampDesc();
    }
}
