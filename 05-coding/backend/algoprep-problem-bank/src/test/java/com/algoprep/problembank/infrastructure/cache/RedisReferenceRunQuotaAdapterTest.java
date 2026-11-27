package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

class RedisReferenceRunQuotaAdapterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final RedisReferenceRunQuotaAdapter adapter;
  private final UUID user = UUID.randomUUID();
  private final UUID request = UUID.randomUUID();

  @SuppressWarnings("unchecked")
  RedisReferenceRunQuotaAdapterTest() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(redis);
    adapter = new RedisReferenceRunQuotaAdapter(provider);
  }

  @SuppressWarnings("unchecked")
  @Test
  void reservesWithTheWindowAndCeilingAndReturnsTheScriptAnswer() {
    when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(0L, 23L);

    assertThat(adapter.reserve(user, request)).isZero();
    assertThat(adapter.reserve(user, request)).isEqualTo(23L);

    verify(redis, org.mockito.Mockito.times(2))
        .execute(
            any(RedisScript.class),
            eq(List.of("problem:reference-run:rate:" + user)),
            eq(request.toString()),
            eq("60"),
            eq("10"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void aRedisFailureOrAnEmptyReplyLetsTheRunGoAhead() {
    when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any()))
        .thenThrow(new IllegalStateException("down"))
        .thenReturn(null);

    assertThat(adapter.reserve(user, request)).isZero();
    assertThat(adapter.reserve(user, request)).isZero();
  }

  @SuppressWarnings("unchecked")
  @Test
  void aRefundRemovesTheSlotAndNeverThrows() {
    ZSetOperations<String, String> zset = mock(ZSetOperations.class);
    when(redis.opsForZSet()).thenReturn(zset);

    adapter.refund(user, request);

    verify(zset).remove("problem:reference-run:rate:" + user, request.toString());
    when(zset.remove(any(), any(Object[].class))).thenThrow(new IllegalStateException("down"));
    adapter.refund(user, request);
  }
}
