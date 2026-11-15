package com.algoprep.identity.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** OAuth state: stored for ten minutes and consumable exactly once (CSRF defence). */
@SuppressWarnings("unchecked")
class RedisOAuthStateAdapterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);
  private final RedisOAuthStateAdapter adapter;

  RedisOAuthStateAdapterTest() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
    adapter = new RedisOAuthStateAdapter(provider);
  }

  @Test
  void storeUsesPrefixAndTenMinuteTtl() {
    adapter.store("abc");
    verify(ops).set("identity:oauthstate:abc", "1", Duration.ofMinutes(10));
  }

  @Test
  void consumeSucceedsOnlyWhenStateExisted() {
    when(ops.getAndDelete("identity:oauthstate:abc")).thenReturn("1", (String) null);
    assertThat(adapter.consume("abc")).isTrue();
    assertThat(adapter.consume("abc")).isFalse();
  }
}
