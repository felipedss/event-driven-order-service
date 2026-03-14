package com.platform.orderservice.mapper;

import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.model.Order;

public class OrderMapper {

  private OrderMapper() {}

  public static OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getOrderId(),
        order.getProductId(),
        order.getQuantity(),
        order.getStatus().name(),
        order.getCancelReason());
  }
}
