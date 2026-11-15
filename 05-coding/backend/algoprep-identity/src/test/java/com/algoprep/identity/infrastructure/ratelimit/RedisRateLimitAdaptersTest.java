package com.algoprep.identity.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Login, class-join and export throttles: thresholds and fail-open behaviour when Redis is down.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class RedisRateLimitAdaptersTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);
  private final ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
  private final ObjectProvider<StringRedisTemplate> absent = mock(ObjectProvider.class);
  private final UUID user = UUID.randomUUID();

  RedisRateLimitAdaptersTest() {
    when(provider.getIfAvailable()).thenReturn(redis);
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
  }

  @Test
  void loginBlocksAtFiveFailuresForTheEmailAndIpPair() {
    var adapter = new RedisLoginAttemptAdapter(provider);
    when(ops.get("identity:ratelimit:login:a@b.c:1.1.1.1")).thenReturn("4", "5", null);
    assertThat(adapter.isBlocked("a@b.c", "1.1.1.1")).isFalse();
    assertThat(adapter.isBlocked("a@b.c", "1.1.1.1")).isTrue();
    assertThat(adapter.isBlocked("a@b.c", "1.1.1.1")).isFalse();
  }

  @Test
  void loginRecordsAndClearsFailures() {
    var adapter = new RedisLoginAttemptAdapter(provider);
    adapter.recordFailure("a@b.c", "ip");
    verify(redis).execute(any(RedisScript.class), eq(List.of("identity:ratelimit:login:a@b.c:ip")));
    adapter.clear("a@b.c", "ip");
    verify(redis).delete("identity:ratelimit:login:a@b.c:ip");
  }

  @Test
  void loginFailsOpenWhenRedisIsMissingOrBroken() {
    var missing = new RedisLoginAttemptAdapter(absent);
    assertThat(missing.isBlocked("e", "ip")).isFalse();
    missing.recordFailure("e", "ip");
    missing.clear("e", "ip");
    when(ops.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
    when(redis.execute(any(RedisScript.class), anyList()))
        .thenThrow(new RedisConnectionFailureException("down"));
    when(redis.delete(anyString())).thenThrow(new RedisConnectionFailureException("down"));
    var broken = new RedisLoginAttemptAdapter(provider);
    assertThat(broken.isBlocked("e", "ip")).isFalse();
    broken.recordFailure("e", "ip");
    broken.clear("e", "ip");
    verify(redis).delete("identity:ratelimit:login:e:ip");
  }

  @Test
  void loginTreatsGarbageCounterAsNotBlocked() {
    when(ops.get(anyString())).thenReturn("abc");
    assertThat(new RedisLoginAttemptAdapter(provider).isBlocked("e", "ip")).isFalse();
  }

  @Test
  void joinBlocksAtFiveFailuresInTheWindow() {
    var adapter = new RedisJoinAttemptLimitAdapter(provider);
    when(redis.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(4L, 5L, null);
    assertThat(adapter.isBlocked(user)).isFalse();
    assertThat(adapter.isBlocked(user)).isTrue();
    assertThat(adapter.isBlocked(user)).isFalse();
    adapter.recordFailure(user);
    verify(redis, org.mockito.Mockito.atLeast(1))
        .execute(
            any(RedisScript.class),
            eq(List.of("identity:class-join:failed:" + user)),
            eq("1"),
            anyString());
    adapter.clear(user);
    verify(redis).delete("identity:class-join:failed:" + user);
  }

  @Test
  void joinFailsOpenWhenRedisIsMissingOrBroken() {
    var missing = new RedisJoinAttemptLimitAdapter(absent);
    assertThat(missing.isBlocked(user)).isFalse();
    missing.recordFailure(user);
    missing.clear(user);
    when(redis.execute(any(RedisScript.class), anyList(), any(), any()))
        .thenThrow(new RedisConnectionFailureException("down"));
    when(redis.delete(anyString())).thenThrow(new RedisConnectionFailureException("down"));
    var broken = new RedisJoinAttemptLimitAdapter(provider);
    assertThat(broken.isBlocked(user)).isFalse();
    broken.clear(user);
    verify(redis).delete("identity:class-join:failed:" + user);
  }

  @Test
  void exportAcquireIsOncePerScopeWithinFiveMinutes() {
    var adapter = new RedisExportRateLimitAdapter(provider);
    String key = "identity:export:" + user + ":profile";
    when(ops.setIfAbsent(key, "1", Duration.ofMinutes(5))).thenReturn(true, false, null);
    assertThat(adapter.tryAcquire(user, "profile")).isTrue();
    assertThat(adapter.tryAcquire(user, "profile")).isFalse();
    assertThat(adapter.tryAcquire(user, "profile")).isFalse();
    adapter.release(user, "profile");
    verify(redis).delete(key);
    verify(redis, never()).delete("identity:export:" + user + ":submissions");
  }
}
