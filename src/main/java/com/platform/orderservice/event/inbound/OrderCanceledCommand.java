package com.platform.orderservice.event.inbound;

public record OrderCanceledCommand(String orderId, String reason) {}
