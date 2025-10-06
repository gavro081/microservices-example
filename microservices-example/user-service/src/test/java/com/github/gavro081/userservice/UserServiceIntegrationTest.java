package com.github.gavro081.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.enums.DebitFailureReason;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
import com.github.gavro081.common.events.BalanceDebitedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.repositories.ProcessedEventRepository;
import com.github.gavro081.userservice.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the UserService.
 * This test uses Testcontainers to spin up real PostgreSQL and RabbitMQ instances.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc // Add this annotation to enable MockMvc
class UserServiceIntegrationTest {
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

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private MockMvc mockMvc; // Inject MockMvc instead of WebTestClient
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void whenUserHasSufficientFunds_thenBalanceIsDebited_andSuccessEventIsPublished() throws IOException {
        // 1. create a user with enough balance in the test database
        User user = new User(null, "test-user", 1000.0);
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getId();

        // 2. create the incoming event
        double orderPrice = 250.0;
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), userId.toString(), "101", "Test Product", 5, 50.0, orderPrice, "test-user"
        );

        // 3. publish the event to the users_queue
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "inventory.reserved", event);

        // 4. use Awaitility to wait for the asynchronous listener to update the user's balance
        await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            User updatedUser = userRepository.findById(userId).orElseThrow();
            // Initial balance was 1000, after debiting 250 it should be 750
            assertThat(updatedUser.getBalance()).isEqualTo(750.0);
        });

        // 5. Verify that a success event was published
        Message resultMessage = rabbitTemplate.receive("orders_queue", 2000); // Listen for the outcome event
        assertThat(resultMessage).isNotNull();
        BalanceDebitedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), BalanceDebitedEvent.class);
        assertThat(resultEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(resultEvent.getTotalPrice()).isEqualTo(orderPrice);
    }

    @Test
    void whenUserHasInsufficientFunds_thenBalanceIsNotChanged_andFailureEventIsPublished() throws IOException {
        User user = new User(null, "poor-user", 50.0); // Not enough balance
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getId();
        double orderPrice = 250.0;
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), userId.toString(), "101", "Test Product", 5, 50.0, orderPrice, "poor-user"
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "inventory.reserved", event);

        // give it a moment, then confirm the balance did NOT change
        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        User finalUser = userRepository.findById(userId).orElseThrow();
        assertThat(finalUser.getBalance()).isEqualTo(50.0); // Should be unchanged

        Message resultMessage = rabbitTemplate.receive("orders_queue", 2000); // Failures are also sent to the orders queue
        assertThat(resultMessage).isNotNull();
        BalanceDebitFailedEvent resultEvent = objectMapper.readValue(resultMessage.getBody(), BalanceDebitFailedEvent.class);
        assertThat(resultEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(resultEvent.getReason()).isEqualTo(DebitFailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void whenGetUserByUsernameIsCalled_thenReturnsCorrectUser() throws Exception { // Add "throws Exception"
        User user = new User(null, "find-me", 123.45);
        userRepository.save(user);

        // Use MockMvc to perform the request
        mockMvc.perform(get("/users/by-username/find-me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }
}

