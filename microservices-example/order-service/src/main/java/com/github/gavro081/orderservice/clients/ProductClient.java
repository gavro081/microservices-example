package com.github.gavro081.orderservice.clients;

import com.github.gavro081.common.dto.ProductDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/by-name/{productName}")
    ProductDetailDto getProductByName(@PathVariable("productName") String productName);
}
