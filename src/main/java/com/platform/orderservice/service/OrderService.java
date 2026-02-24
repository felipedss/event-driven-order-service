package com.platform.orderservice.service;

import com.platform.orderservice.command.CreateOrderCommand;
import com.platform.orderservice.event.outbound.OrderCreatedEvent;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.messaging.producer.KafkaProducerService;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final KafkaProducerService kafkaProducer;

  public Order createOrder(CreateOrderCommand command) {
    Order order = Order.builder().quantity(command.quantity()).status(OrderStatus.CREATED).build();
    Order saved = orderRepository.save(order);
    kafkaProducer.send(
        "order.created", saved.getOrderId(), new OrderCreatedEvent(saved.getOrderId()));
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

  public void cancelOrder(String orderId) {
    Order order =
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(OrderStatus.CANCELED);
    orderRepository.save(order);
    log.info("Order {} canceled", orderId);
  }
}
