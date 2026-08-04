package com.platform.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.exception.IdempotencyConflictException;
import com.platform.orderservice.model.IdempotencyKey;
import com.platform.orderservice.repository.IdempotencyKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

  private final IdempotencyKeyRepository repository;
  private final ObjectMapper objectMapper;

  public String computeHash(String rawBody) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(rawBody.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Looks up a cached response for the given idempotency key and payload hash.
   *
   * @return the cached OrderResponse if the key+hash match a previous request
   * @throws IdempotencyConflictException if the key exists but the hash differs (client bug)
   */
  public Optional<OrderResponse> findCached(String idempotencyKey, String payloadHash) {
    return repository
        .findById(idempotencyKey)
        .map(
            record -> {
              if (!record.getPayloadHash().equals(payloadHash)) {
                throw new IdempotencyConflictException(idempotencyKey);
              }
              log.info("Idempotency cache hit for key={}", idempotencyKey);
              return deserialize(record.getResponse());
            });
  }

  public void store(String idempotencyKey, String payloadHash, OrderResponse response) {
    IdempotencyKey record =
        IdempotencyKey.builder()
            .idempotencyKey(idempotencyKey)
            .payloadHash(payloadHash)
            .response(serialize(response))
            .createdAt(Instant.now())
            .build();
    repository.save(record);
    log.info("Stored idempotency key={}", idempotencyKey);
  }

  private String serialize(OrderResponse response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize OrderResponse", e);
    }
  }

  private OrderResponse deserialize(String json) {
    try {
      return objectMapper.readValue(json, OrderResponse.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize cached OrderResponse", e);
    }
  }
}
