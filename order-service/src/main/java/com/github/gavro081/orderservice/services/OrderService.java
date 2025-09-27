package com.github.gavro081.orderservice.services;

import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getOrders(){
        return orderRepository.findAll();
    }
}
