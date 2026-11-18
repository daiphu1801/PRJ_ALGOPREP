package com.algoprep.identity.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** BR-25 step 1: ten per hour, the window starts at the first change, Redis trouble fails open. */
@SuppressWarnings("unchecked")
class RedisAvatarRateLimitAdapterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);
  private final ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
  private final RedisAvatarRateLimitAdapter adapter = new RedisAvatarRateLimitAdapter(provider);
  private final UUID user = UUID.randomUUID();
  private final String key = "identity:avatar:rate:" + user;

  RedisAvatarRateLimitAdapterTest() {
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
  }

  @Test
  void theFirstChangeStartsTheHourWindow() {
    when(ops.increment(key)).thenReturn(1L);

    assertThat(adapter.tryAcquire(user)).isTrue();

    verify(redis).expire(key, Duration.ofHours(1));
  }

  @Test
  void theTenthChangeIsAllowedAndTheEleventhIsNot() {
    when(ops.increment(key)).thenReturn(10L, 11L);

    assertThat(adapter.tryAcquire(user)).isTrue();
    assertThat(adapter.tryAcquire(user)).isFalse();
    verify(redis, never()).expire(any(), any(Duration.class));
  }

  @Test
  void aRedisFailureOrNullAllowsTheChange() {
    doThrow(new RedisConnectionFailureException("down")).when(ops).increment(key);
    assertThat(adapter.tryAcquire(user)).isTrue();

    doReturn(null).when(ops).increment(key);
    assertThat(adapter.tryAcquire(user)).isTrue();
  }
}
