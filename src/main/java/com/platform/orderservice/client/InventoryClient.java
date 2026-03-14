package com.platform.orderservice.client;

import com.platform.orderservice.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

  private final RestClient inventoryRestClient;

  public InventoryItemResponse getProduct(String productId) {
    try {
      return inventoryRestClient
          .get()
          .uri("/api/v1/inventory/{productId}", productId)
          .retrieve()
          .body(InventoryItemResponse.class);
    } catch (HttpClientErrorException.NotFound e) {
      throw new ProductNotFoundException(productId);
    }
  }
}
