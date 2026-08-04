package com.platform.orderservice.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

  @Test
  void startsWithFullCapacity() {
    var bucket = new TokenBucket(5, 1.0);

    for (int i = 0; i < 5; i++) {
      assertThat(bucket.tryConsume()).isTrue();
    }
  }

  @Test
  void rejectsRequestWhenEmpty() {
    var bucket = new TokenBucket(3, 1.0);
    bucket.tryConsume();
    bucket.tryConsume();
    bucket.tryConsume();

    assertThat(bucket.tryConsume()).isFalse();
  }

  @Test
  void refillsTokensAfterElapsedTime() throws InterruptedException {
    var bucket = new TokenBucket(5, 5.0); // 5 tokens/sec
    for (int i = 0; i < 5; i++) bucket.tryConsume();
    assertThat(bucket.tryConsume()).isFalse();

    Thread.sleep(500); // ~2.5 new tokens

    assertThat(bucket.tryConsume()).isTrue();
  }

  @Test
  void doesNotExceedCapacityAfterLongIdle() throws InterruptedException {
    var bucket = new TokenBucket(3, 10.0); // refills fast
    for (int i = 0; i < 3; i++) bucket.tryConsume();

    Thread.sleep(1000); // would add 10 tokens, but cap is 3

    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse(); // 4th is over cap
  }

  @Test
  void capacityOfOneAllowsOneRequestAtATime() throws InterruptedException {
    var bucket = new TokenBucket(1, 2.0); // 1 token, refills at 2/sec

    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();

    Thread.sleep(600); // ~1.2 tokens earned → 1 available

    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();
  }
}
