package com.platform.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.orderservice.controller.response.OrderResponse;
import com.platform.orderservice.exception.IdempotencyConflictException;
import com.platform.orderservice.model.IdempotencyKey;
import com.platform.orderservice.repository.IdempotencyKeyRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  @Mock private IdempotencyKeyRepository repository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @InjectMocks private IdempotencyService idempotencyService;

  @Test
  void computeHash_returnsDeterministicHexString() {
    String hash1 = idempotencyService.computeHash("{\"productId\":\"p-1\",\"quantity\":3}");
    String hash2 = idempotencyService.computeHash("{\"productId\":\"p-1\",\"quantity\":3}");

    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(64).matches("[0-9a-f]+");
  }

  @Test
  void computeHash_returnsDifferentHash_forDifferentInputs() {
    String hash1 = idempotencyService.computeHash("{\"productId\":\"p-1\",\"quantity\":3}");
    String hash2 = idempotencyService.computeHash("{\"productId\":\"p-2\",\"quantity\":3}");

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  void computeHash_throwsIllegalStateException_whenSha256Unavailable() {
    try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
      mocked
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));

      assertThatThrownBy(() -> idempotencyService.computeHash("any"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SHA-256 not available");
    }
  }

  @Test
  void findCached_returnsEmpty_whenKeyNotFound() {
    when(repository.findById("key-abc")).thenReturn(Optional.empty());

    Optional<OrderResponse> result = idempotencyService.findCached("key-abc", "any-hash");

    assertThat(result).isEmpty();
  }

  @Test
  void findCached_returnsCachedResponse_whenKeyAndHashMatch() throws Exception {
    OrderResponse expected = new OrderResponse("order-1", "p-1", 3, "CREATED", null);
    String hash = idempotencyService.computeHash("{\"productId\":\"p-1\",\"quantity\":3}");
    String serialized = objectMapper.writeValueAsString(expected);

    when(repository.findById("key-abc"))
        .thenReturn(
            Optional.of(
                IdempotencyKey.builder()
                    .idempotencyKey("key-abc")
                    .payloadHash(hash)
                    .response(serialized)
                    .createdAt(Instant.now())
                    .build()));

    Optional<OrderResponse> result = idempotencyService.findCached("key-abc", hash);

    assertThat(result).isPresent();
    assertThat(result.get().orderId()).isEqualTo("order-1");
    assertThat(result.get().productId()).isEqualTo("p-1");
    assertThat(result.get().quantity()).isEqualTo(3);
    assertThat(result.get().status()).isEqualTo("CREATED");
  }

  @Test
  void findCached_throwsIdempotencyConflictException_whenHashDiffers() {
    when(repository.findById("key-abc"))
        .thenReturn(
            Optional.of(
                IdempotencyKey.builder()
                    .idempotencyKey("key-abc")
                    .payloadHash("original-hash")
                    .response("{}")
                    .createdAt(Instant.now())
                    .build()));

    assertThatThrownBy(() -> idempotencyService.findCached("key-abc", "different-hash"))
        .isInstanceOf(IdempotencyConflictException.class)
        .hasMessageContaining("key-abc");
  }

  @Test
  void store_savesRecordWithCorrectFields() {
    OrderResponse response = new OrderResponse("order-1", "p-1", 3, "CREATED", null);
    String hash = idempotencyService.computeHash("{\"productId\":\"p-1\",\"quantity\":3}");

    idempotencyService.store("key-abc", hash, response);

    ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
    verify(repository).save(captor.capture());

    IdempotencyKey saved = captor.getValue();
    assertThat(saved.getIdempotencyKey()).isEqualTo("key-abc");
    assertThat(saved.getPayloadHash()).isEqualTo(hash);
    assertThat(saved.getResponse()).contains("order-1");
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void store_throwsIllegalStateException_whenSerializationFails() throws Exception {
    doThrow(new JsonProcessingException("simulated error") {})
        .when(objectMapper)
        .writeValueAsString(any());

    OrderResponse response = new OrderResponse("order-1", "p-1", 3, "CREATED", null);

    assertThatThrownBy(() -> idempotencyService.store("key-abc", "hash", response))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to serialize");
  }

  @Test
  void findCached_throwsIllegalStateException_whenStoredResponseIsCorrupted() {
    String hash = "some-hash";
    when(repository.findById("key-abc"))
        .thenReturn(
            Optional.of(
                IdempotencyKey.builder()
                    .idempotencyKey("key-abc")
                    .payloadHash(hash)
                    .response("not-valid-json")
                    .createdAt(Instant.now())
                    .build()));

    assertThatThrownBy(() -> idempotencyService.findCached("key-abc", hash))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to deserialize");
  }
}
