package com.platform.orderservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.platform.orderservice.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

class InventoryClientTest {

  private static final String BASE_URL = "http://localhost:8082";

  private MockRestServiceServer server;
  private InventoryClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    RestClient restClient =
        RestClient.builder()
            .baseUrl(BASE_URL)
            .requestFactory(restTemplate.getRequestFactory())
            .build();
    client = new InventoryClient(restClient); // uses Lombok-generated constructor
  }

  @Test
  void getProduct_returnsInventoryItem_whenProductExists() {
    server
        .expect(requestTo(BASE_URL + "/api/v1/inventory/prod-1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"itemId\":\"item-1\",\"productId\":\"prod-1\",\"availableQuantity\":10}",
                MediaType.APPLICATION_JSON));

    InventoryItemResponse result = client.getProduct("prod-1");

    assertThat(result.productId()).isEqualTo("prod-1");
    assertThat(result.itemId()).isEqualTo("item-1");
    assertThat(result.availableQuantity()).isEqualTo(10);
    server.verify();
  }

  @Test
  void getProduct_throwsProductNotFoundException_whenProductNotFound() {
    server
        .expect(requestTo(BASE_URL + "/api/v1/inventory/unknown"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> client.getProduct("unknown"))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining("unknown");
    server.verify();
  }
}
