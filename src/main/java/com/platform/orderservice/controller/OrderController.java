package com.platform.orderservice.controller;

import com.platform.orderservice.controller.request.CreateOrderCommand;
import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.mapper.OrderMapper;
import com.platform.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public OrderResponse create(@RequestBody CreateOrderCommand command) {
    return OrderMapper.toResponse(orderService.createOrder(command));
  }

  @GetMapping("/{orderId}")
  public OrderResponse get(@PathVariable String orderId) {
    return OrderMapper.toResponse(orderService.getOrder(orderId));
  }
}
