package com.github.gavro081.productservice.models;

import jakarta.persistence.*;
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
public class ProcessedEvent {
    @Id
    private UUID eventId;

    Instant timestamp;
}
