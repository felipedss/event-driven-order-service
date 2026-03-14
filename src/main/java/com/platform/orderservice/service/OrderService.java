package com.platform.orderservice.service;

import com.platform.orderservice.client.InventoryClient;
import com.platform.orderservice.controller.request.CreateOrderCommand;
import com.platform.orderservice.event.outbound.OrderCreatedEvent;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.messaging.producer.KafkaProducerService;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  @Value("${topics.order.created}")
  private String topicOrderCreated;

  private final OrderRepository orderRepository;
  private final KafkaProducerService kafkaProducer;
  private final InventoryClient inventoryClient;

  public Order createOrder(CreateOrderCommand command) {
    if (command.productId() == null || command.productId().isBlank()) {
      throw new IllegalArgumentException("productId must not be null or blank");
    }
    if (command.quantity() == null || command.quantity() <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
    inventoryClient.getProduct(command.productId());
    Order order =
        Order.builder()
            .productId(command.productId())
            .quantity(command.quantity())
            .status(OrderStatus.CREATED)
            .build();
    Order saved = orderRepository.save(order);
    kafkaProducer.send(
        topicOrderCreated,
        saved.getOrderId(),
        new OrderCreatedEvent(saved.getOrderId(), saved.getProductId(), saved.getQuantity()));
    log.info("Order {} created and published to Kafka", saved.getOrderId());
    return saved;
  }

  public Order getOrder(String orderId) {
    return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
  }

  public void confirmOrder(String orderId) {
    Order order =
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(OrderStatus.CONFIRMED);
    orderRepository.save(order);
    log.info("Order {} confirmed", orderId);
  }

  public void cancelOrder(String orderId, String reason) {
    Order order =
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(OrderStatus.CANCELED);
    order.setCancelReason(reason);
    orderRepository.save(order);
    log.info("Order {} canceled, reason: {}", orderId, reason);
  }
}
