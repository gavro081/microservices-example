package com.github.gavro081.productservice.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.services.ProductService;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    String getIndex(){
        return "hello from product service";
    }

    @GetMapping("/products")
    List<Product> getProducts(){
        return productService.getProducts();
    }
}
