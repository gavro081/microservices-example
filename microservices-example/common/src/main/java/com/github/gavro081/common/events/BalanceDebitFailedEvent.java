package com.github.gavro081.common.events;

import com.github.gavro081.common.enums.DebitFailureReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class BalanceDebitFailedEvent implements Serializable {
        private final UUID eventId = UUID.randomUUID();

        private final UUID orderId;
        private final String productId;
        private final int quantity;
        private final DebitFailureReason reason;
        private final String message;
        private final String username;
}
