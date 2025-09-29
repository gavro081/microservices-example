package com.github.gavro081.orderservice.controllers;

import com.github.gavro081.orderservice.dao.OrderRequest;
import com.github.gavro081.orderservice.models.Order;
import com.github.gavro081.orderservice.services.OrderService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping()
    List<Order> getOrders(){
        return orderService.getOrders();
    }

    @PostMapping()
    ResponseEntity<UUID> postOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        UUID orderId = orderService.createOrder(orderRequest);
        if (orderId == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.accepted().body(orderId);
    }

}
