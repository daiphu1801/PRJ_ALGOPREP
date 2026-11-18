package com.algoprep.aireview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/** The acquire and release contract; the Lua script itself runs on real Redis in bootstrap. */
class RedisOwnerLockTest {
  private final UUID id = UUID.randomUUID();
  private final StringRedisTemplate template = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mockOps();
  private final RedisOwnerLock lock;

  @SuppressWarnings("unchecked")
  RedisOwnerLockTest() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(template);
    when(template.opsForValue()).thenReturn(ops);
    lock = new RedisOwnerLock(provider, "ai:session:");
  }

  @SuppressWarnings("unchecked")
  private static ValueOperations<String, String> mockOps() {
    return mock(ValueOperations.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void aWinnerRunsTheWorkAndReleasesWithItsOwnToken() {
    when(ops.setIfAbsent(any(), any(), any(java.time.Duration.class))).thenReturn(true);
    Optional<String> out = lock.withLock(id, () -> "done");
    assertThat(out).contains("done");
    var token = ArgumentCaptor.forClass(String.class);
    verify(ops).setIfAbsent(eq("ai:session:" + id), token.capture(), eq(RedisOwnerLock.TTL));
    verify(template)
        .execute(
            any(RedisScript.class),
            eq(java.util.List.of("ai:session:" + id)),
            eq(token.getValue()));
    assertThat(RedisOwnerLock.TTL).isGreaterThan(java.time.Duration.ofSeconds(120));
  }

  @Test
  @SuppressWarnings("unchecked")
  void aLoserGetsEmptyAndNeverReleasesTheOthersLock() {
    when(ops.setIfAbsent(any(), any(), any(java.time.Duration.class))).thenReturn(false);
    boolean[] ran = {false};
    assertThat(lock.withLock(id, () -> ran[0] = true)).isEmpty();
    assertThat(ran[0]).isFalse();
    verify(template, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void theLockIsReleasedWhenTheWorkFailsAndAReleaseErrorIsSwallowed() {
    when(ops.setIfAbsent(any(), any(), any(java.time.Duration.class))).thenReturn(true);
    when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
        .thenThrow(new IllegalStateException("redis down"));
    assertThatThrownBy(
            () ->
                lock.withLock(
                    id,
                    () -> {
                      throw new IllegalArgumentException("llm failed");
                    }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("llm failed");
    verify(template).execute(any(RedisScript.class), anyList(), any(Object[].class));
  }

  @Test
  void redisDownWhileAcquiringFailsClosedWithAir501() {
    when(ops.setIfAbsent(any(), any(), any(java.time.Duration.class)))
        .thenThrow(new IllegalStateException("redis down"));
    assertThatThrownBy(() -> lock.withLock(id, () -> "x"))
        .isInstanceOf(AiProviderUnavailableException.class)
        .extracting(failure -> ((AiProviderUnavailableException) failure).reason())
        .isEqualTo(AiProviderUnavailableException.Reason.UPSTREAM_PORT);
  }
}
