package com.platform.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.exception.GlobalExceptionHandler;
import com.platform.orderservice.exception.IdempotencyConflictException;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.exception.ProductNotFoundException;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.service.IdempotencyService;
import com.platform.orderservice.service.OrderService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  private static final String IDEMPOTENCY_KEY = "test-key-123";
  private static final String BODY = "{\"productId\":\"p-1\",\"quantity\":3}";

  @Mock private OrderService orderService;
  @Mock private IdempotencyService idempotencyService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @InjectMocks private OrderController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void postOrder_returns200_withCreatedOrder() throws Exception {
    Order order = Order.builder().orderId("id-1").quantity(3).status(OrderStatus.CREATED).build();
    when(idempotencyService.findCached(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.empty());
    when(orderService.createOrder(any())).thenReturn(order);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("id-1"))
        .andExpect(jsonPath("$.status").value("CREATED"));
  }

  @Test
  void postOrder_returns200_fromCacheOnRetry() throws Exception {
    OrderResponse cached = new OrderResponse("id-1", "p-1", 3, OrderStatus.CREATED.name(), null);
    when(idempotencyService.findCached(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.of(cached));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("id-1"));
  }

  @Test
  void postOrder_returns409_whenSameKeyDifferentPayload() throws Exception {
    when(idempotencyService.findCached(eq(IDEMPOTENCY_KEY), any()))
        .thenThrow(new IdempotencyConflictException(IDEMPOTENCY_KEY));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"different\",\"quantity\":99}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString(IDEMPOTENCY_KEY)));
  }

  @Test
  void postOrder_returns400_whenIdempotencyKeyIsMissing() throws Exception {
    mockMvc
        .perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Required header 'X-Idempotency-Key' is missing"));
  }

  @Test
  void postOrder_returns400_whenProductIdIsBlank() throws Exception {
    when(idempotencyService.findCached(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.empty());
    when(orderService.createOrder(any()))
        .thenThrow(new IllegalArgumentException("productId must not be null or blank"));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("productId must not be null or blank"));
  }

  @Test
  void postOrder_returns404_whenProductNotFound() throws Exception {
    when(idempotencyService.findCached(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.empty());
    when(orderService.createOrder(any()))
        .thenThrow(new ProductNotFoundException("unknown-product"));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"unknown-product\",\"quantity\":1}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void getOrder_returns200_whenFound() throws Exception {
    Order order = Order.builder().orderId("id-1").quantity(3).status(OrderStatus.CONFIRMED).build();
    when(orderService.getOrder("id-1")).thenReturn(order);

    mockMvc
        .perform(get("/api/v1/orders/id-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void getOrder_returns404_whenNotFound() throws Exception {
    when(orderService.getOrder("missing")).thenThrow(new OrderNotFoundException("missing"));

    mockMvc
        .perform(get("/api/v1/orders/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
