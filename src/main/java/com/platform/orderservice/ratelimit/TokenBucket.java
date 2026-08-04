package com.platform.orderservice.ratelimit;

public class TokenBucket {

  private final int capacity;
  private final double refillRatePerSecond;
  private double tokens;
  private long lastRefillNanos;

  public TokenBucket(int capacity, double refillRatePerSecond) {
    this.capacity = capacity;
    this.refillRatePerSecond = refillRatePerSecond;
    this.tokens = capacity;
    this.lastRefillNanos = System.nanoTime();
  }

  public synchronized boolean tryConsume() {
    refill();
    if (tokens >= 1.0) {
      tokens -= 1.0;
      return true;
    }
    return false;
  }

  private void refill() {
    long now = System.nanoTime();
    double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
    tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
    lastRefillNanos = now;
  }
}
