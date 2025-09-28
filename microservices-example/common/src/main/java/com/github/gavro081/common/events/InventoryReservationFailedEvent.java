package com.github.gavro081.common.events;

import com.github.gavro081.common.enums.FailureReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class InventoryReservationFailedEvent implements Serializable {
    private final UUID eventId = UUID.randomUUID();

    private final UUID orderId;
    private final String productId;
    private final FailureReason reason;
    private final String message;
}