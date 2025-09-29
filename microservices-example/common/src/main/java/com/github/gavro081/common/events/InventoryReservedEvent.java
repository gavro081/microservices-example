package com.github.gavro081.common.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter @Setter
public class InventoryReservedEvent implements Serializable {
    private final UUID eventId = UUID.randomUUID();

    private final UUID orderId;
    private final String userId;
    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;
    private final double totalPrice;
    private final String username;
}
