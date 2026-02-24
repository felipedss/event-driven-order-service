package com.platform.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.orderservice.command.CreateOrderCommand;
import com.platform.orderservice.event.outbound.OrderCreatedEvent;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.messaging.producer.KafkaProducerService;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private KafkaProducerService kafkaProducer;
  @InjectMocks private OrderService orderService;

  @Test
  void createOrder_persistsOrderAndPublishesEvent() {
    Order saved =
        Order.builder().orderId("abc-123").quantity(5).status(OrderStatus.CREATED).build();
    when(orderRepository.save(any(Order.class))).thenReturn(saved);

    Order result = orderService.createOrder(new CreateOrderCommand(5));

    assertThat(result.getOrderId()).isEqualTo("abc-123");
    assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);

    ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(kafkaProducer).send(eq("order.created"), eq("abc-123"), captor.capture());
    assertThat(captor.getValue().orderId()).isEqualTo("abc-123");
  }

  @Test
  void getOrder_returnsOrder_whenFound() {
    Order order =
        Order.builder().orderId("abc-123").quantity(3).status(OrderStatus.CREATED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    assertThat(orderService.getOrder("abc-123")).isEqualTo(order);
  }

  @Test
  void getOrder_throwsOrderNotFoundException_whenNotFound() {
    when(orderRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getOrder("missing"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessageContaining("missing");
  }

  @Test
  void confirmOrder_updatesStatusToConfirmed() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CREATED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.confirmOrder("abc-123");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    verify(orderRepository).save(order);
  }

  @Test
  void cancelOrder_updatesStatusToCanceled() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CREATED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.cancelOrder("abc-123");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    verify(orderRepository).save(order);
  }

  @Test
  void confirmOrder_throwsOrderNotFoundException_whenNotFound() {
    when(orderRepository.findById("gone")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.confirmOrder("gone"))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void cancelOrder_throwsOrderNotFoundException_whenNotFound() {
    when(orderRepository.findById("gone")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.cancelOrder("gone"))
        .isInstanceOf(OrderNotFoundException.class);
  }
}
