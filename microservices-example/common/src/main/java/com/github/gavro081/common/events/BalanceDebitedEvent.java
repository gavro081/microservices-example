package com.github.gavro081.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class BalanceDebitedEvent {
    private final UUID eventId = UUID.randomUUID();

    private final UUID orderId;
    private final Long userId;
    private final String productId;
    private final double totalPrice;
    private final String productName;
    private final String username;
}
