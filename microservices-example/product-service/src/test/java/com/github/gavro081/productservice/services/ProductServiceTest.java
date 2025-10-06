package com.github.gavro081.productservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.ReservationFailureReason;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
import com.github.gavro081.common.events.InventoryReservationFailedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProcessedEventService processedEventService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ProductService productService;

    private OrderCreatedEvent reserveEvent;
    private BalanceDebitFailedEvent freeEvent;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        reserveEvent = new OrderCreatedEvent(UUID.randomUUID(), "101", "1", 5, "test-user");
        freeEvent = new BalanceDebitFailedEvent(UUID.randomUUID(), "101", 5, null, null, "test-user");
        sampleProduct = new Product(101L, "Test Product", "test", 20.0, 10);
    }

    @Test
    void reserveInventory_whenStockIsSufficient_shouldDecreaseStockAndPublishSuccess() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(processedEventService).markActionAsProcessed(any(), any());

        productService.reserveInventory(reserveEvent);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getQuantity()).isEqualTo(5);

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("inventory.reserved"), any(InventoryReservedEvent.class));
    }

    @Test
    void reserveInventory_whenStockIsInsufficient_shouldPublishFailure() {
        sampleProduct.setQuantity(2);
        when(productRepository.findById(101L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(processedEventService).markActionAsProcessed(any(), any());

        productService.reserveInventory(reserveEvent);

        verify(productRepository, never()).save(any());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("inventory.failed"), any(InventoryReservationFailedEvent.class));
    }

    @Test
    void reserveInventory_whenEventIsDuplicate_shouldDoNothing() {
        doThrow(DataIntegrityViolationException.class).when(processedEventService).markActionAsProcessed(any(), any());

        productService.reserveInventory(reserveEvent);

        verifyNoInteractions(productRepository);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void freeInventory_whenCalled_shouldIncreaseStock() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(processedEventService).markActionAsProcessed(any(), any());

        productService.freeInventory(freeEvent);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getQuantity()).isEqualTo(15);
    }
}
