package com.github.gavro081.orderservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.gavro081.orderservice.repositories.OrderRepository;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final OrderRepository orderRepository;

    @Bean
    CommandLineRunner seedOrderData() {
        return args -> {
            // Only seed if database is empty
            if (orderRepository.count() == 0) {
                log.info("Order database is empty - ready for new orders");
                // You can add sample orders here if needed
                
                // Example:
                // List<Order> orders = List.of(
                //     Order.builder()
                //         .userId(1L)
                //         .productId(1L)
                //         .quantity(1)
                //         .status(OrderStatus.COMPLETED)
                //         .timestamp(Instant.now().minus(5, ChronoUnit.DAYS))
                //         .build()
                // );
                // orderRepository.saveAll(orders);
            } else {
                log.info("Order data already exists: {} orders", orderRepository.count());
            }
        };
    }
}
