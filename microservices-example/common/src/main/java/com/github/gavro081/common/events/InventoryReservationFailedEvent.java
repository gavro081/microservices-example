package com.github.gavro081.common.events;

import com.github.gavro081.common.enums.ReservationFailureReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class InventoryReservationFailedEvent implements Serializable {
    private final UUID eventId = UUID.randomUUID();

    private final UUID orderId;
    private final String productId;
    private final ReservationFailureReason reason;
    private final String message;
}