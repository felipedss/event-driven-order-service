package com.platform.orderservice.controller;

import com.platform.orderservice.command.CreateOrderCommand;
import com.platform.orderservice.model.Order;
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
  public Order create(@RequestBody CreateOrderCommand command) {
    return orderService.createOrder(command);
  }

  @GetMapping("/{orderId}")
  public Order get(@PathVariable String orderId) {
    return orderService.getOrder(orderId);
  }
}
