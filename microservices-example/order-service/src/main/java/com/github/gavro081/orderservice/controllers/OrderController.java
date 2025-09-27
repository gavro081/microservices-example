package com.github.gavro081.orderservice.controllers;

import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.services.OrderService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    ResponseEntity<Void> postOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        orderService.createOrder(orderRequest);
        return ResponseEntity.accepted().build();
    }

}
