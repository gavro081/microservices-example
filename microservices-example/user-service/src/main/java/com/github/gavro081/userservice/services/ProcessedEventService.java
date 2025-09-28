package com.github.gavro081.userservice.services;

import com.github.gavro081.userservice.models.ProcessedEvent;
import com.github.gavro081.userservice.repositories.ProcessedEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    void markActionAsProcessed(UUID orderId, String context){
        ProcessedEvent event = ProcessedEvent.builder()
                .orderId(orderId)
                .context(context)
                .timestamp(Instant.now())
                .build();
        processedEventRepository.save(event);
    }
}
