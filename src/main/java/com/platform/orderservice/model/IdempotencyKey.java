package com.platform.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity(name = "idempotency_keys")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

  @Id private String idempotencyKey;

  @Column(nullable = false)
  private String payloadHash;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String response;

  @Column(nullable = false)
  private Instant createdAt;
}
