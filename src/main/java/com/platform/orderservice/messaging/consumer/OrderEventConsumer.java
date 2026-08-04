package com.platform.orderservice.messaging.consumer;

import com.platform.orderservice.event.inbound.OrderCanceledCommand;
import com.platform.orderservice.event.inbound.OrderConfirmedCommand;
import com.platform.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

  private final OrderService orderService;

  @KafkaListener(
      topics = "${topics.order.confirmed}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void handleOrderConfirmed(OrderConfirmedCommand command, Acknowledgment ack) {
    log.info("Received order.confirmed for orderId={}", command.orderId());
    orderService.confirmOrder(command.orderId());
    ack.acknowledge();
  }

  @KafkaListener(topics = "${topics.order.canceled}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleOrderCancelled(OrderCanceledCommand event, Acknowledgment ack) {
    log.info("Received order.canceled for orderId={}", event.orderId());
    orderService.cancelOrder(event.orderId(), event.reason());
    ack.acknowledge();
  }
}
