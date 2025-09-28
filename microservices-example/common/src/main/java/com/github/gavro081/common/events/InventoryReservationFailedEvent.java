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
    private final UUID eventId;
    private String productId;
    private FailureReason reason;
    private String message;
}