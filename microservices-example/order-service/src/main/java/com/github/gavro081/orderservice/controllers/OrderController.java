package com.github.gavro081.orderservice.controllers;

import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.services.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/")
    String getIndex(){
        return "hello from order service";
    }

    @GetMapping("/orders")
    List<Order> getOrders(){
        return orderService.getOrders();
    }

    @PostMapping("/orders")
    String postOrder(){
        return "OK";
    }

}
