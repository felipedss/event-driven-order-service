package com.platform.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.orderservice.client.InventoryClient;
import com.platform.orderservice.client.InventoryItemResponse;
import com.platform.orderservice.controller.request.CreateOrderCommand;
import com.platform.orderservice.event.outbound.OrderCreatedEvent;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.exception.ProductNotFoundException;
import com.platform.orderservice.messaging.producer.KafkaProducerService;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private KafkaProducerService kafkaProducer;
  @Mock private InventoryClient inventoryClient;
  @InjectMocks private OrderService orderService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(orderService, "topicOrderCreated", "order.created");
  }

  @Test
  void createOrder_persistsOrderAndPublishesEvent() {
    when(inventoryClient.getProduct("product-A"))
        .thenReturn(new InventoryItemResponse("item-1", "product-A", 10));
    Order saved =
        Order.builder()
            .orderId("abc-123")
            .productId("product-A")
            .quantity(5)
            .status(OrderStatus.CREATED)
            .build();
    when(orderRepository.save(any(Order.class))).thenReturn(saved);

    Order result = orderService.createOrder(new CreateOrderCommand("product-A", 5));

    assertThat(result.getOrderId()).isEqualTo("abc-123");
    assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);

    ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(kafkaProducer).send(eq("order.created"), eq("abc-123"), captor.capture());
    assertThat(captor.getValue().orderId()).isEqualTo("abc-123");
    assertThat(captor.getValue().productId()).isEqualTo("product-A");
  }

  @Test
  void createOrder_throwsIllegalArgumentException_whenProductIdIsNull() {
    assertThatThrownBy(() -> orderService.createOrder(new CreateOrderCommand(null, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("productId");
  }

  @Test
  void createOrder_throwsIllegalArgumentException_whenProductIdIsBlank() {
    assertThatThrownBy(() -> orderService.createOrder(new CreateOrderCommand("  ", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("productId");
  }

  @Test
  void createOrder_throwsIllegalArgumentException_whenQuantityIsNull() {
    assertThatThrownBy(() -> orderService.createOrder(new CreateOrderCommand("product-A", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("quantity");
  }

  @Test
  void createOrder_throwsIllegalArgumentException_whenQuantityIsZeroOrNegative() {
    assertThatThrownBy(() -> orderService.createOrder(new CreateOrderCommand("product-A", 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("quantity");
  }

  @Test
  void createOrder_throwsProductNotFoundException_whenProductDoesNotExist() {
    when(inventoryClient.getProduct("unknown-product"))
        .thenThrow(new ProductNotFoundException("unknown-product"));

    assertThatThrownBy(() -> orderService.createOrder(new CreateOrderCommand("unknown-product", 1)))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining("unknown-product");
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

  // ── confirmOrder ────────────────────────────────────────────────────────────

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
  void confirmOrder_skipsUpdate_whenOrderAlreadyConfirmed() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CONFIRMED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.confirmOrder("abc-123");

    verify(orderRepository, never()).save(any());
  }

  @Test
  void confirmOrder_skipsUpdate_whenOrderAlreadyCanceled() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CANCELED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.confirmOrder("abc-123");

    verify(orderRepository, never()).save(any());
  }

  @Test
  void confirmOrder_throwsOrderNotFoundException_whenNotFound() {
    when(orderRepository.findById("gone")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.confirmOrder("gone"))
        .isInstanceOf(OrderNotFoundException.class);
  }

  // ── cancelOrder ─────────────────────────────────────────────────────────────

  @Test
  void cancelOrder_updatesStatusAndReasonToCanceled() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CREATED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.cancelOrder("abc-123", "payment failed");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.getCancelReason()).isEqualTo("payment failed");
    verify(orderRepository).save(order);
  }

  @Test
  void cancelOrder_updatesStatusWithNullReason() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CREATED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.cancelOrder("abc-123", null);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.getCancelReason()).isNull();
    verify(orderRepository).save(order);
  }

  @Test
  void cancelOrder_skipsUpdate_whenOrderAlreadyCanceled() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CANCELED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.cancelOrder("abc-123", "duplicate");

    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_skipsUpdate_whenOrderAlreadyConfirmed() {
    Order order =
        Order.builder().orderId("abc-123").quantity(2).status(OrderStatus.CONFIRMED).build();
    when(orderRepository.findById("abc-123")).thenReturn(Optional.of(order));

    orderService.cancelOrder("abc-123", "duplicate");

    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_throwsOrderNotFoundException_whenNotFound() {
    when(orderRepository.findById("gone")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.cancelOrder("gone", null))
        .isInstanceOf(OrderNotFoundException.class);
  }
}
