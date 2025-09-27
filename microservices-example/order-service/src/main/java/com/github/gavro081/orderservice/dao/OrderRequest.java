package com.github.gavro081.orderservice.dao;


public record OrderRequest(String username, String productName, String quantity) {
}