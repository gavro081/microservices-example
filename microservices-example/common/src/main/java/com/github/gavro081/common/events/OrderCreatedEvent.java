package com.github.gavro081.common.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


@AllArgsConstructor
@Getter @Setter
public class OrderCreatedEvent implements Serializable {
    private final UUID eventId = UUID.randomUUID();
    private final Instant timestamp = Instant.now();

    private final UUID orderId;
    private final String productId;
    private final String userId;
    private final int quantity;
    private final String username;

}
