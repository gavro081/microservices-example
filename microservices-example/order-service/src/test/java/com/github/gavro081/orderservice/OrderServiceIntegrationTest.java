package com.github.gavro081.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.dto.ProductDetailDto;
import com.github.gavro081.common.dto.UserDetailDto;
import com.github.gavro081.common.events.OrderCreatedEvent;
import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.models.OrderStatus;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the OrderService.
 * This test uses a real RabbitMQ instance and mocks the downstream HTTP services (Products, Users) using WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWireMock(port = 0) // Starts WireMock on a random port
class OrderServiceIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Dynamically configure properties to point to our test containers and mock server.
     */
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        // WireMock for downstream services
        registry.add("microservices.product-service.url", () -> "http://localhost:${wiremock.server.port}");
        registry.add("microservices.user-service.url", () -> "http://localhost:${wiremock.server.port}");
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        // Clean up queues before each test
        rabbitTemplate.receive("products_queue", 10);
    }

    @Test
    void whenCreateOrderIsCalled_thenOrderIsSaved_andEventIsPublished() throws IOException {
        // 1. Configure WireMock to stub the downstream service calls
        long productId = 101L;
        long userId = 1L;
        String productName = "TestBook";
        String username = "test-user";

        stubFor(WireMock.get(urlEqualTo("/products/by-name/" + productName))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new ProductDetailDto(productId, 25.50)))));

        stubFor(WireMock.get(urlEqualTo("/users/by-username/" + username))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new UserDetailDto(userId)))));

        // 2. Create the API request body
        OrderRequest orderRequest = new OrderRequest(username, productName, "2");

        // 3. Make the API call to our running OrderService
        webTestClient.post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isAccepted(); // Assert HTTP status is 202 Accepted

        // 4. Verify the event was published to RabbitMQ
        Message resultMessage = rabbitTemplate.receive("products_queue", 2000);
        assertThat(resultMessage).isNotNull();
        OrderCreatedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), OrderCreatedEvent.class);

        assertThat(resultEvent.getProductId()).isEqualTo(String.valueOf(productId));
        assertThat(resultEvent.getUserId()).isEqualTo(String.valueOf(userId));
        assertThat(resultEvent.getQuantity()).isEqualTo(2);

        // 5. Verify the order was saved to the database with PENDING status
        UUID orderId = resultEvent.getOrderId();
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Order savedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(savedOrder.getProductId()).isEqualTo(productId);
        });
    }
}
