package com.platform.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.platform.orderservice.exception.GlobalExceptionHandler;
import com.platform.orderservice.exception.OrderNotFoundException;
import com.platform.orderservice.model.Order;
import com.platform.orderservice.model.OrderStatus;
import com.platform.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  @Mock private OrderService orderService;
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
    Order order =
        Order.builder().orderId("id-1").quantity(3).status(OrderStatus.CREATED).build();
    when(orderService.createOrder(any())).thenReturn(order);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("id-1"))
        .andExpect(jsonPath("$.status").value("CREATED"));
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

    mockMvc.perform(get("/api/v1/orders/missing")).andExpect(status().isNotFound());
  }
}
