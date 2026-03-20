package com.platform.orderservice.exception;

public class IdempotencyConflictException extends RuntimeException {

  public IdempotencyConflictException(String idempotencyKey) {
    super(
        "Idempotency key '"
            + idempotencyKey
            + "' was already used with a different payload. Use a new key for a different request.");
  }
}
