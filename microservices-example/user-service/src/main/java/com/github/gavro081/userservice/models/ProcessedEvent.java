package com.github.gavro081.userservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@IdClass(ProcessedEventId.class)
public class ProcessedEvent {
    @Id
    private UUID orderId;

    @Id
    private String context;

    Instant timestamp;
}
