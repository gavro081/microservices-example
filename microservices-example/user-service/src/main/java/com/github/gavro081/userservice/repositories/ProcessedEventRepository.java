package com.github.gavro081.userservice.repositories;

import com.github.gavro081.userservice.models.ProcessedEvent;
import com.github.gavro081.userservice.models.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
}
