package com.github.gavro081.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.ReservationFailureReason;
import com.github.gavro081.common.events.InventoryReservationFailedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


/**
 * Integration test for the ProductService.
 * This test uses Testcontainers to spin up real PostgreSQL and RabbitMQ instances.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers // Enables Testcontainers support for JUnit 5
class ProductServiceIntegrationTest {

    // --- Container Setup ---

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    // --- Test Dependencies ---

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ObjectMapper objectMapper; // For deserializing messages

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void whenStockIsSufficient_thenInventoryIsReserved_andSuccessEventIsPublished() throws IOException {
        // --- Arrange ---
        Product product = new Product(null, "Test Book", "tech", 15.99, 20);
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();
        int quantityToReserve = 5;
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), productId.toString(), "1", quantityToReserve, "test-user");

        // --- Act ---
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "order.created", event);

        // --- Assert Database State ---
        await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getQuantity()).isEqualTo(15);
        });

        // --- Assert Published Event ---
        Message resultMessage = rabbitTemplate.receive("users_queue", 2000); // Listen on the next queue in the saga
        assertThat(resultMessage).isNotNull();
        InventoryReservedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), InventoryReservedEvent.class);
        assertThat(resultEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(resultEvent.getQuantity()).isEqualTo(quantityToReserve);
        assertThat(resultEvent.getTotalPrice()).isEqualTo(15.99 * 5);
    }

    @Test
    void whenStockIsInsufficient_thenInventoryIsNotChanged_andFailureEventIsPublished() throws IOException {
        // --- Arrange ---
        Product product = new Product(null, "Test Gadget", "test", 199.99, 2);
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();
        int quantityToReserve = 5; // More than available
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), productId.toString(), "1", quantityToReserve, "test-user");

        // --- Act ---
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "order.created", event);

        // --- Assert Database State ---
        // Give it a moment to process, then confirm the quantity did NOT change.
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getQuantity()).isEqualTo(2); // Should be unchanged

        // --- Assert Published Event ---
        Message resultMessage = rabbitTemplate.receive("orders_queue", 2000); // Listen on the orders queue for failures
        assertThat(resultMessage).isNotNull();
        InventoryReservationFailedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), InventoryReservationFailedEvent.class);
        assertThat(resultEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(resultEvent.getReason()).isEqualTo(ReservationFailureReason.INSUFFICIENT_STOCK);
    }
}

