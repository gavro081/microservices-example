package com.github.gavro081.orderservice.repositories;

import com.github.gavro081.orderservice.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Order findTopByTimestampIsNotNullOrderByTimestampDesc();
}
