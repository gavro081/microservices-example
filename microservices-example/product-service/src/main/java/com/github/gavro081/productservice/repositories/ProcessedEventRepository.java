package com.github.gavro081.productservice.repositories;

import com.github.gavro081.productservice.models.ProcessedEvent;
import com.github.gavro081.productservice.models.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
}
