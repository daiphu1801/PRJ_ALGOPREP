package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.dto.TestcaseUploadSession.Confirmation;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Session;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Side;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Slot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisTestcaseUploadSessionAdapterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = valueOps();
  private final UUID id = UUID.randomUUID();
  private final Session pending =
      new Session(
          id,
          UUID.randomUUID(),
          UUID.randomUUID(),
          List.of(
              new Slot(
                  "a",
                  UUID.randomUUID(),
                  "SAMPLE",
                  null,
                  new Side(10, "problems/p/testcases/t/input-1.txt"),
                  new Side(20, "problems/p/testcases/t/expected_output-2.txt"))),
          null);
  private final RedisTestcaseUploadSessionAdapter adapter;

  @SuppressWarnings("unchecked")
  RedisTestcaseUploadSessionAdapterTest() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
    adapter = new RedisTestcaseUploadSessionAdapter(provider);
  }

  @SuppressWarnings("unchecked")
  private static ValueOperations<String, String> valueOps() {
    return mock(ValueOperations.class);
  }

  @Test
  void aSessionRoundTripsThroughJsonUnderItsKeyWithTheGivenLifetime() {
    var json = ArgumentCaptor.forClass(String.class);

    adapter.save(pending, Duration.ofMinutes(20));

    verify(ops).set(eq("problem:upload:" + id), json.capture(), eq(Duration.ofMinutes(20)));
    when(ops.get("problem:upload:" + id)).thenReturn(json.getValue());
    assertThat(adapter.find(id)).contains(pending);
    assertThat(json.getValue()).doesNotContain("input.txt content");
  }

  @Test
  void aConfirmedSessionKeepsItsConfirmation() {
    var done = pending.confirmedWith(new Confirmation(5, Instant.parse("2026-11-06T10:00:00Z")));
    var json = ArgumentCaptor.forClass(String.class);
    adapter.save(done, Duration.ofMinutes(1));
    verify(ops).set(anyString(), json.capture(), eq(Duration.ofMinutes(1)));
    when(ops.get("problem:upload:" + id)).thenReturn(json.getValue());

    var read = adapter.find(id).orElseThrow();

    assertThat(read.confirmed()).isTrue();
    assertThat(read.confirmation().testcaseSetVersion()).isEqualTo(5);
  }

  @Test
  void anUnknownOrExpiredSessionIsEmpty() {
    assertThat(adapter.find(UUID.randomUUID())).isEmpty();
  }

  @Test
  void anUpdateKeepsTheTimeTheSessionHasLeft() {
    when(redis.getExpire("problem:upload:" + id, TimeUnit.MILLISECONDS)).thenReturn(90_000L);

    adapter.update(pending);

    verify(ops)
        .setIfPresent(eq("problem:upload:" + id), anyString(), eq(Duration.ofMillis(90_000)));
  }

  @Test
  void anUpdateOfASessionThatIsGoneWritesNothing() {
    when(redis.getExpire("problem:upload:" + id, TimeUnit.MILLISECONDS)).thenReturn(-2L);

    adapter.update(pending);

    verify(ops, never())
        .setIfPresent(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
  }

  @Test
  void aStoreFailurePropagatesSoTheCallerCanAnswer503() {
    when(ops.get(anyString())).thenThrow(new IllegalStateException("down"));

    assertThatThrownBy(() -> adapter.find(id)).isInstanceOf(IllegalStateException.class);
  }
}
