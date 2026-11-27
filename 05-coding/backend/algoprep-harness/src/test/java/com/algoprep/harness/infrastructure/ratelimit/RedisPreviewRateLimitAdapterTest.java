package com.algoprep.harness.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisPreviewRateLimitAdapterTest {
  @SuppressWarnings("unchecked")
  private static RedisPreviewRateLimitAdapter adapter(StringRedisTemplate template) {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(template);
    return new RedisPreviewRateLimitAdapter(provider);
  }

  @Test
  @SuppressWarnings("unchecked")
  void aRedisFailureLetsTheCallThrough() {
    var template = mock(StringRedisTemplate.class);
    when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
        .thenThrow(new IllegalStateException("redis down"));
    assertThat(adapter(template).acquire(UUID.randomUUID())).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  void aWaitFromTheScriptIsReturnedAndANullReplyIsAllowed() {
    var template = mock(StringRedisTemplate.class);
    when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
        .thenReturn(1L, (Long) null);
    var adapter = adapter(template);
    assertThat(adapter.acquire(UUID.randomUUID())).isEqualTo(1);
    assertThat(adapter.acquire(UUID.randomUUID())).isZero();
  }
}
