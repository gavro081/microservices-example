package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderService orderService;

    private void setupWebClientMocks() {
        // Configure mocks for the WebClient chain
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    void createOrder_whenUserAndProductExist_shouldSaveOrderAndPublishEvent() {
        setupWebClientMocks();
        OrderRequest request = new OrderRequest("test-user", "test-product", "2");
        UserDetailDto userDto = new UserDetailDto(1L);
        ProductDetailDto productDto = new ProductDetailDto(101L, 25.0);

        when(responseSpec.bodyToMono(eq(UserDetailDto.class))).thenReturn(Mono.just(userDto));
        when(responseSpec.bodyToMono(eq(ProductDetailDto.class))).thenReturn(Mono.just(productDto));

        Order savedOrder = Order.builder().id(UUID.randomUUID()).userId(1L).productId(101L).quantity(2).status(OrderStatus.PENDING).build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UUID orderId = orderService.createOrder(request);

        assertThat(orderId).isNotNull();
        assertThat(orderId).isEqualTo(savedOrder.getId());
        verify(orderRepository).save(any(Order.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("order.created"), any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_whenUserNotFound_shouldReturnNull() {
        setupWebClientMocks();
        OrderRequest request = new OrderRequest("test-user", "test-product", "2");

        when(responseSpec.bodyToMono(eq(UserDetailDto.class))).thenReturn(Mono.empty());
        when(responseSpec.bodyToMono(eq(ProductDetailDto.class))).thenReturn(Mono.just(new ProductDetailDto(101L, 25.0)));

        UUID orderId = orderService.createOrder(request);

        assertThat(orderId).isNull();
        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateOrderStatus_whenOrderIsPending_shouldUpdateStatusAndNotify() {
        UUID orderId = UUID.randomUUID();
        Order pendingOrder = Order.builder().id(orderId).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        orderService.updateOrderStatus("test-user", orderId, OrderStatus.COMPLETED);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(capturedOrder.getTimestamp()).isNotNull();

        verify(notificationService).notifyOrderStatusUpdate("test-user", orderId, OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderStatus_whenOrderIsAlreadyCompleted_shouldDoNothing() {
        UUID orderId = UUID.randomUUID();
        Order completedOrder = Order.builder().id(orderId).status(OrderStatus.COMPLETED).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        orderService.updateOrderStatus("test-user", orderId, OrderStatus.FAILED);

        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).notifyOrderStatusUpdate(any(), any(), any());
    }
}

