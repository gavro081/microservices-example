//package com.github.gavro081.orderservice;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.gavro081.common.dto.ProductDetailDto;
//import com.github.gavro081.common.dto.UserDetailDto;
//import com.github.gavro081.common.events.OrderCreatedEvent;
//import com.github.gavro081.orderservice.dao.OrderRequest;
//import com.github.gavro081.orderservice.models.Order;
//import com.github.gavro081.orderservice.models.OrderStatus;
//import com.github.gavro081.orderservice.repositories.OrderRepository;
//import com.github.tomakehurst.wiremock.client.WireMock;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.testcontainers.containers.RabbitMQContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.io.IOException;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
///**
// * Integration test for the OrderService.
// * This test uses a real RabbitMQ instance and mocks the downstream HTTP services (Products, Users) using WireMock.
// * This version is configured to work with Feign clients.
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//@AutoConfigureWireMock(port = 0)
//class OrderServiceIntegrationTest {
//
//    @Container
//    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");
//
//    @Autowired
//    private WebTestClient webTestClient;
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//    @Autowired
//    private OrderRepository orderRepository;
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @DynamicPropertySource
//    static void setProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
//        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
//
//        registry.add("eureka.client.enabled", () -> "false");
//        registry.add("spring.cloud.loadbalancer.enabled", () -> "false");
//
//        registry.add("feign.client.config.product-service.url", () -> "http://localhost:${wiremock.server.port}");
//        registry.add("feign.client.config.user-service.url", () -> "http://localhost:${wiremock.server.port}");
//    }
//
//    @BeforeEach
//    void setUp() {
//        orderRepository.deleteAll();
//        // Clean up queues before each test
//        rabbitTemplate.receive("products_queue", 10);
//    }
//
//    @Test
//    void whenCreateOrderIsCalled_thenOrderIsSaved_andEventIsPublished() throws IOException {
//        // Arrange: Configure WireMock to stub the downstream service calls
//        long productId = 101L;
//        long userId = 1L;
//        String productName = "TestBook";
//        String username = "test-user";
//
//        stubFor(WireMock.get(urlEqualTo("/products/by-name/" + productName))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                        .withBody(objectMapper.writeValueAsString(new ProductDetailDto(productId, 25.50)))));
//
//        stubFor(WireMock.get(urlEqualTo("/users/by-username/" + username))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                        .withBody(objectMapper.writeValueAsString(new UserDetailDto(userId)))));
//
//        OrderRequest orderRequest = new OrderRequest(username, productName, "2");
//
//        // Act
//        webTestClient.post().uri("/orders")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(orderRequest)
//                .exchange()
//                .expectStatus().isCreated(); // Assert HTTP status is 201 Created
//
//        // Assert: Verify the event was published to RabbitMQ
//        Message resultMessage = rabbitTemplate.receive("products_queue", 5000);
//        assertThat(resultMessage).isNotNull();
//        OrderCreatedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), OrderCreatedEvent.class);
//
//        assertThat(resultEvent.getProductId()).isEqualTo(String.valueOf(productId));
//        assertThat(resultEvent.getUserId()).isEqualTo(String.valueOf(userId));
//        assertThat(resultEvent.getQuantity()).isEqualTo(2);
//
//        // Assert: Verify the order was saved to the database with PENDING status
//        UUID orderId = resultEvent.getOrderId();
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            Order savedOrder = orderRepository.findById(orderId).orElseThrow();
//            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
//            assertThat(savedOrder.getProductId()).isEqualTo(productId);
//        });
//    }
//
//    @Test
//    void whenCreateOrderWithNonexistentProduct_thenReturns404() throws JsonProcessingException {
//        // Arrange: Configure WireMock for a 404 on product, but success on user
//        String productName = "NonExistentProduct";
//        String username = "test-user";
//        long userId = 1L;
//
//        // Stub the failing product call
//        stubFor(WireMock.get(urlEqualTo("/products/by-name/" + productName))
//                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
//
//        // Stub the successful user call
//        stubFor(WireMock.get(urlEqualTo("/users/by-username/" + username))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                        .withBody(objectMapper.writeValueAsString(new UserDetailDto(userId)))));
//
//        OrderRequest orderRequest = new OrderRequest(username, productName, "1");
//
//        // Act & Assert: Make the API call and assert for 404 Not Found
//        webTestClient.post().uri("/orders")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(orderRequest)
//                .exchange()
//                .expectStatus().isNotFound();
//
//        // Assert: Verify no order was saved and no event was published
//        assertThat(orderRepository.count()).isZero();
//        assertThat(rabbitTemplate.receive("products_queue", 100)).isNull();
//    }
//}
//
