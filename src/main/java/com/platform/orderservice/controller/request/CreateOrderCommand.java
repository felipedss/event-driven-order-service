package com.platform.orderservice.controller.request;

public record CreateOrderCommand(String productId, Integer quantity) {}
