package com.platform.orderservice.event.outbound;

public record OrderCreatedEvent(String orderId, String productId, int quantity) {}
