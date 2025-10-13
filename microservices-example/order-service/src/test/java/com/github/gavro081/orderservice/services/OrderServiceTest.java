package com.github.gavro081.orderservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.clients.ProductClient;
import com.github.gavro081.orderservice.clients.UserClient;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.exceptions.ExternalServiceException;
import com.github.gavro081.orderservice.exceptions.ResourceNotFoundException;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OrderService.
 * This version mocks the Feign clients (UserClient, ProductClient).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserClient userClient; // Mock the Feign client
    @Mock
    private ProductClient productClient; // Mock the Feign client

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_whenUserAndProductExist_shouldSaveOrderAndPublishEvent() {
        // Arrange
        OrderRequest request = new OrderRequest("test-user", "test-product", "2");
        UserDetailDto userDto = new UserDetailDto(1L);
        ProductDetailDto productDto = new ProductDetailDto(101L, 25.0);

        when(userClient.getUserByUsername("test-user")).thenReturn(userDto);
        when(productClient.getProductByName("test-product")).thenReturn(productDto);

        Order savedOrder = Order.builder().id(UUID.randomUUID()).userId(1L).productId(101L).quantity(2).status(OrderStatus.PENDING).build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        UUID orderId = orderService.createOrder(request);

        // Assert
        assertThat(orderId).isNotNull();
        assertThat(orderId).isEqualTo(savedOrder.getId());
        verify(orderRepository).save(any(Order.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("order.created"), any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_whenUserNotFound_shouldThrowException() {
        // Arrange
        OrderRequest request = new OrderRequest("nonexistent-user", "test-product", "2");
        when(userClient.getUserByUsername("nonexistent-user")).thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify no interactions with other dependencies
        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void createOrder_whenProductNotFound_shouldThrowException() {
        // Arrange
        OrderRequest request = new OrderRequest("test-user", "nonexistent-product", "2");
        UserDetailDto userDto = new UserDetailDto(1L);
        // Mock the successful user call
        when(userClient.getUserByUsername("test-user")).thenReturn(userDto);
        // Mock the failing product call
        when(productClient.getProductByName("nonexistent-product")).thenThrow(new ResourceNotFoundException("Product not found"));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void createOrder_whenExternalServiceFails_shouldThrowException() {
        // Arrange
        OrderRequest request = new OrderRequest("test-user", "test-product", "2");
        when(userClient.getUserByUsername("test-user")).thenThrow(new ExternalServiceException("Service unavailable", new RuntimeException()));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Service unavailable");

        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateOrderStatus_whenOrderIsPending_shouldUpdateStatusAndNotify() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order pendingOrder = Order.builder().id(orderId).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        // Act
        orderService.updateOrderStatus("test-user", orderId, OrderStatus.COMPLETED);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(capturedOrder.getTimestamp()).isNotNull();

        verify(notificationService).notifyOrderStatusUpdate("test-user", orderId, OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderStatus_whenOrderIsAlreadyCompleted_shouldDoNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order completedOrder = Order.builder().id(orderId).status(OrderStatus.COMPLETED).timestamp(Instant.now()).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        // Act
        orderService.updateOrderStatus("test-user", orderId, OrderStatus.FAILED);

        // Assert
        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).notifyOrderStatusUpdate(anyString(), any(), any());
    }
}

