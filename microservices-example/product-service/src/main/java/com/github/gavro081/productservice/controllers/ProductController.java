package com.github.gavro081.productservice.controllers;

import java.util.ArrayList;
import java.util.List;

import com.github.gavro081.common.dto.ProductDetailDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.gavro081.productservice.models.Product;
import com.github.gavro081.productservice.services.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping()
    List<Product> getProducts(){
    return productService.getProducts();
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ProductDetailDto> getProductByName(@PathVariable String name){
        Product product = productService.getProductByName(name);
        if (product != null){
            ProductDetailDto dto = new ProductDetailDto(product.getId(), product.getPrice());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
