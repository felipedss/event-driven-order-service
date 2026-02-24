package com.platform.orderservice.messaging.consumer;

import static org.mockito.Mockito.verify;

import com.platform.orderservice.event.inbound.OrderCancelledEvent;
import com.platform.orderservice.event.inbound.OrderConfirmedEvent;
import com.platform.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

  @Mock private OrderService orderService;
  @InjectMocks private OrderEventConsumer consumer;

  @Test
  void handleOrderConfirmed_delegatesToOrderService() {
    consumer.handleOrderConfirmed(new OrderConfirmedEvent("order-1"));
    verify(orderService).confirmOrder("order-1");
  }

  @Test
  void handleOrderCancelled_delegatesToOrderService() {
    consumer.handleOrderCancelled(new OrderCancelledEvent("order-2"));
    verify(orderService).cancelOrder("order-2");
  }
}
