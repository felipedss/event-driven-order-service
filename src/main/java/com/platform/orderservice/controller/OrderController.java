package com.platform.orderservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.orderservice.controller.request.CreateOrderCommand;
import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.mapper.OrderMapper;
import com.platform.orderservice.service.IdempotencyService;
import com.platform.orderservice.service.OrderService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final IdempotencyService idempotencyService;
  private final ObjectMapper objectMapper;

  @PostMapping
  public OrderResponse create(
      @RequestBody String rawBody, @RequestHeader("X-Idempotency-Key") String idempotencyKey)
      throws JsonProcessingException {

    CreateOrderCommand command = objectMapper.readValue(rawBody, CreateOrderCommand.class);

    String hash = idempotencyService.computeHash(rawBody);
    Optional<OrderResponse> cached = idempotencyService.findCached(idempotencyKey, hash);
    if (cached.isPresent()) {
      return cached.get();
    }

    OrderResponse response = OrderMapper.toResponse(orderService.createOrder(command));
    idempotencyService.store(idempotencyKey, hash, response);
    return response;
  }

  @GetMapping("/{orderId}")
  public OrderResponse get(@PathVariable String orderId) {
    return OrderMapper.toResponse(orderService.getOrder(orderId));
  }
}
