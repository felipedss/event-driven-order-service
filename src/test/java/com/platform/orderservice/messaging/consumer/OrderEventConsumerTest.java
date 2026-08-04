package com.platform.orderservice.messaging.consumer;

import static org.mockito.Mockito.verify;

import com.platform.orderservice.event.inbound.OrderCanceledCommand;
import com.platform.orderservice.event.inbound.OrderConfirmedCommand;
import com.platform.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

  @Mock private OrderService orderService;
  @Mock private Acknowledgment ack;
  @InjectMocks private OrderEventConsumer consumer;

  @Test
  void handleOrderConfirmed_delegatesToOrderService() {
    consumer.handleOrderConfirmed(new OrderConfirmedCommand("order-1"), ack);
    verify(orderService).confirmOrder("order-1");
    verify(ack).acknowledge();
  }

  @Test
  void handleOrderCancelled_delegatesToOrderService() {
    consumer.handleOrderCancelled(
        new OrderCanceledCommand("order-2", "inventory unavailable"), ack);
    verify(orderService).cancelOrder("order-2", "inventory unavailable");
    verify(ack).acknowledge();
  }
}
