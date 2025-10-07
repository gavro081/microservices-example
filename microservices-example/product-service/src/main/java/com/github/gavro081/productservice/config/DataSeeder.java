package com.github.gavro081.productservice.config;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.repositories.ProductRepository;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final ProductRepository productRepository;

    @Bean
    CommandLineRunner seedProductData() {
        return args -> {
            if (productRepository.count() == 0) {
                log.info("Seeding product data...");

                List<Product> products = List.of(
                        Product.builder().name("macbook").category("electronics").price(1999.99).quantity(50).build(),
                        Product.builder().name("mouse").category("electronics").price(29.99).quantity(200).build(),
                        Product.builder().name("keyboard").category("electronics").price(149.99).quantity(75).build(),
                        Product.builder().name("chair").category("furniture").price(299.99).quantity(30).build(),
                        Product.builder().name("desk").category("furniture").price(599.99).quantity(15).build(),
                        Product.builder().name("mug").category("kitchen").price(12.99).quantity(100).build(),
                        Product.builder().name("bottle").category("kitchen").price(24.99).quantity(150).build(),
                        Product.builder().name("backpack").category("accessories").price(89.99).quantity(120).build()
                );

                productRepository.saveAll(products);
                log.info("Seeded {} products successfully", products.size());
            } else {
                log.info("Product data already exists, skipping seed");
            }
        };
    }
}
