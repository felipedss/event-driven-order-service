package com.platform.orderservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ExternalClientConfig {

  private final InventoryClientProperties inventoryProperties;

  @Bean
  public RestClient inventoryRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(inventoryProperties.connectTimeout());
    factory.setReadTimeout(inventoryProperties.readTimeout());
    return RestClient.builder().baseUrl(inventoryProperties.url()).requestFactory(factory).build();
  }
}
