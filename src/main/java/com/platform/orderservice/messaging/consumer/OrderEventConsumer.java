package com.platform.orderservice.messaging.consumer;

import com.platform.orderservice.event.inbound.OrderCancelledEvent;
import com.platform.orderservice.event.inbound.OrderConfirmedEvent;
import com.platform.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

  private final OrderService orderService;

  @KafkaListener(topics = "order.confirmed", groupId = "${spring.kafka.consumer.group-id}")
  public void handleOrderConfirmed(OrderConfirmedEvent event) {
    log.info("Received order.confirmed for orderId={}", event.orderId());
    orderService.confirmOrder(event.orderId());
  }

  @KafkaListener(topics = "order.cancelled", groupId = "${spring.kafka.consumer.group-id}")
  public void handleOrderCancelled(OrderCancelledEvent event) {
    log.info("Received order.cancelled for orderId={}", event.orderId());
    orderService.cancelOrder(event.orderId());
  }
}
