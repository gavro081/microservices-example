package com.github.gavro081.productservice.services;

import com.github.gavro081.productservice.models.ProcessedEvent;
import com.github.gavro081.productservice.repositories.ProcessedEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    void markEventAsProcessed(UUID eventId){
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .timestamp(Instant.now())
                .build();
        processedEventRepository.save(event);
    }
}
