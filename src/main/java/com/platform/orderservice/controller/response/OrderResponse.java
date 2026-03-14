package com.platform.orderservice.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record OrderResponse(
    String orderId, String productId, Integer quantity, String status, String cancelReason) {}
