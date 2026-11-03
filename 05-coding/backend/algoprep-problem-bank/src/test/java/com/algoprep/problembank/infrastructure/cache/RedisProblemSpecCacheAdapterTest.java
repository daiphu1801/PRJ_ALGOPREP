package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RedisProblemSpecCacheAdapterTest {
  @BeforeEach
  void start() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void stop() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletesOnlyAfterCommitAndToleratesUnavailableRedis() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    var redis = mock(StringRedisTemplate.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    var adapter = new RedisProblemSpecCacheAdapter(provider);
    UUID id = UUID.randomUUID();
    adapter.invalidateAfterCommit(id);
    verify(redis, never()).delete("problem:spec:" + id);
    assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    var callback = TransactionSynchronizationManager.getSynchronizations().getFirst();
    callback.afterCommit();
    verify(redis).delete("problem:spec:" + id);
    when(provider.getIfAvailable()).thenReturn(null);
    assertThatCode(callback::afterCommit).doesNotThrowAnyException();
    when(provider.getIfAvailable()).thenThrow(new IllegalStateException("Offline fixture"));
    assertThatCode(callback::afterCommit).doesNotThrowAnyException();
  }
}
