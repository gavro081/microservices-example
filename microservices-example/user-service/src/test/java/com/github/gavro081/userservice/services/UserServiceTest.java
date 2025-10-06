package com.github.gavro081.userservice.services;

import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.BalanceDebitFailedEvent;
import com.github.gavro081.common.events.BalanceDebitedEvent;
import com.github.gavro081.common.events.InventoryReservedEvent;
import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private UserService userService;

    private InventoryReservedEvent sampleEvent;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleEvent = new InventoryReservedEvent(
                UUID.randomUUID(),
                "1",
                "101",
                "test-product",
                4,
                25.0,
                100.0,
                "test-username"
        );
        sampleUser = new User(1L, "test-user", 150.0);
    }

    @Test
    void whenUserHasSufficientBalance_thenBalanceIsDebitedAndSuccessEventIsPublished(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        doNothing().when(processedEventService).markActionAsProcessed(any(UUID.class), anyString());

        userService.debitUserBalance(sampleEvent);
        assertThat(sampleUser.getBalance()).isEqualTo(50.0);

        verify(userRepository).save(sampleUser);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("balance.success"),
                any(BalanceDebitedEvent.class)
        );

        verify(rabbitTemplate, never()).convertAndSend(anyString(), eq("balance.failed"), Optional.ofNullable(any()));
    }

    @Test
    void whenUserHasInsufficientBalance_thenFailureEventIsPublished() {
        sampleUser.setBalance(50.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        doNothing().when(processedEventService).markActionAsProcessed(any(UUID.class), anyString());

        userService.debitUserBalance(sampleEvent);

        assertThat(sampleUser.getBalance()).isEqualTo(50.0);
        verify(userRepository, never()).save(any(User.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("balance.failed"), any(BalanceDebitFailedEvent.class));
    }

    @Test
    void whenUserIsNotFound_thenFailureEventIsPublished() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        doNothing().when(processedEventService).markActionAsProcessed(any(UUID.class), anyString());

        userService.debitUserBalance(sampleEvent);

        verify(userRepository, never()).save(any(User.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("balance.failed"), any(BalanceDebitFailedEvent.class));
    }

    @Test
    void whenEventIsDuplicate_thenProcessingIsSkipped() {
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(processedEventService).markActionAsProcessed(sampleEvent.getOrderId(), "BALANCE_DEBIT");

        userService.debitUserBalance(sampleEvent);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(rabbitTemplate);

        verify(processedEventService).markActionAsProcessed(sampleEvent.getOrderId(), "BALANCE_DEBIT");
    }
}
