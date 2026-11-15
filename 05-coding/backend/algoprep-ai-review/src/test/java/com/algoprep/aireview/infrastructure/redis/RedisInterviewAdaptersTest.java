package com.algoprep.aireview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.ChatMessage.Kind;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** Chat memory and sweep-retry adapters: normal path plus the Redis-down degradation (BR-AI-10). */
class RedisInterviewAdaptersTest {
  private final UUID id = UUID.randomUUID();
  private final StringRedisTemplate template = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mockOps();
  private final ObjectProvider<StringRedisTemplate> provider = provider(template);

  @SuppressWarnings("unchecked")
  private static ValueOperations<String, String> mockOps() {
    return mock(ValueOperations.class);
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate t) {
    ObjectProvider<StringRedisTemplate> p = mock(ObjectProvider.class);
    when(p.getObject()).thenReturn(t);
    return p;
  }

  @Test
  void chatMemoryRoundTripsAsOneJsonValueWithTtl() {
    when(template.opsForValue()).thenReturn(ops);
    var adapter = new RedisInterviewChatMemoryAdapter(provider, 3);
    var messages = List.of(new ChatMessage(Kind.PROBLEM, "p"), new ChatMessage(Kind.USER, "hi"));

    adapter.write(id, messages);

    var json = ArgumentCaptor.forClass(String.class);
    verify(ops).set(eq("ai:chatmemory:" + id), json.capture(), eq(Duration.ofHours(3)));
    when(ops.get("ai:chatmemory:" + id)).thenReturn(json.getValue());
    assertThat(adapter.read(id)).contains(messages);
  }

  @Test
  void chatMemoryMissAndCorruptValueBothMeanRebuild() {
    when(template.opsForValue()).thenReturn(ops);
    var adapter = new RedisInterviewChatMemoryAdapter(provider, 2);

    assertThat(adapter.read(id)).isEmpty();
    when(ops.get(anyString())).thenReturn("not json");
    assertThat(adapter.read(id)).isEmpty();
  }

  @Test
  void chatMemoryRedisFailuresNeverReachTheCaller() {
    when(template.opsForValue()).thenThrow(new IllegalStateException("redis down"));
    var adapter = new RedisInterviewChatMemoryAdapter(provider, 2);

    adapter.write(id, List.of(new ChatMessage(Kind.AI, "x")));

    assertThat(adapter.read(id)).isEmpty();
  }

  @Test
  void sweepRetryCountsAndSetsOneDayTtl() {
    when(template.opsForValue()).thenReturn(ops);
    when(ops.increment("ai:sweep-retry:" + id)).thenReturn(3L, (Long) null);
    var adapter = new RedisInterviewSweepRetryAdapter(provider);

    assertThat(adapter.recordFailure(id)).isEqualTo(3);
    assertThat(adapter.recordFailure(id)).isEqualTo(1);
    verify(template, times(2)).expire(eq("ai:sweep-retry:" + id), eq(Duration.ofDays(1)));

    adapter.clear(id);
    verify(template).delete("ai:sweep-retry:" + id);
  }

  @Test
  void sweepRetryRedisOutageAnswersOneSoNoSessionIsAbandoned() {
    when(template.opsForValue()).thenThrow(new IllegalStateException("redis down"));
    when(template.delete(anyString())).thenThrow(new IllegalStateException("redis down"));
    var adapter = new RedisInterviewSweepRetryAdapter(provider);

    assertThat(adapter.recordFailure(id)).isEqualTo(1);
    adapter.clear(id);
    verify(template).delete(any(String.class));
  }

  @Test
  void sessionAndReviewLocksUseTheirOwnKeyPrefix() {
    when(template.opsForValue()).thenReturn(ops);
    when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

    assertThat(new RedisInterviewSessionLock(provider).withLock(id, () -> "s")).contains("s");
    assertThat(new RedisSolutionReviewLock(provider).withLock(id, () -> "r")).contains("r");

    verify(ops).setIfAbsent(eq("ai:session:" + id), anyString(), any(Duration.class));
    verify(ops).setIfAbsent(eq("ai:review:" + id), anyString(), any(Duration.class));
  }
}
