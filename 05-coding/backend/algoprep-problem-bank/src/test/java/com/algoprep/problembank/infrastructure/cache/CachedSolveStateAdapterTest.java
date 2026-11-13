package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CachedSolveStateAdapterTest {
  @Test
  @SuppressWarnings("unchecked")
  void cachePartitionsUsersAndUnavailableStateIsRetriedRatherThanCachedAsEmpty() {
    var origin = mock(SolveStatePort.class);
    ObjectProvider<SolveStatePort> source = mock(ObjectProvider.class);
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    var entries = new HashMap<String, String>();
    when(source.getObject()).thenReturn(origin);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(call -> entries.get(call.getArgument(0)));
    org.mockito.Mockito.doAnswer(
            call -> {
              entries.put(call.getArgument(0), call.getArgument(1));
              assertThat(call.<Duration>getArgument(2)).isEqualTo(Duration.ofSeconds(60));
              return null;
            })
        .when(values)
        .set(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Duration.class));
    var adapter =
        new CachedSolveStateAdapter(
            source, new CatalogReadCache(provider, mock(ObjectProvider.class)));
    UUID alice = UUID.randomUUID();
    UUID bob = UUID.randomUUID();
    UUID solved = UUID.randomUUID();
    var aliceState = new SolveStates(Set.of(solved), Set.of());
    var bobState = new SolveStates(Set.of(), Set.of(solved));
    when(origin.mine(alice)).thenReturn(Optional.of(aliceState));
    when(origin.mine(bob)).thenReturn(Optional.empty(), Optional.of(bobState));
    assertThat(adapter.mine(alice)).contains(aliceState);
    assertThat(adapter.mine(bob)).isEmpty();
    assertThat(adapter.mine(bob)).contains(bobState);
    assertThat(adapter.mine(alice)).contains(aliceState);
    assertThat(adapter.mine(bob)).contains(bobState);
    verify(origin).mine(alice);
    verify(origin, times(2)).mine(bob);
    assertThat(entries.keySet())
        .containsExactlyInAnyOrder("problem:solvestate:" + alice, "problem:solvestate:" + bob);
  }
}
