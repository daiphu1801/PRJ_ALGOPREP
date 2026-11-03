package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RedisProblemListVersionAdapterTest {
  @AfterEach
  void clear() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  void invalidationWaitsForCommitAndRedisErrorsAreBestEffort() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
    ProblemListVersionPort adapter = new RedisProblemListVersionAdapter(provider);
    TransactionSynchronizationManager.initSynchronization();
    adapter.incrementAfterCommit();
    verifyNoInteractions(redis);
    var synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
    synchronization.afterCommit();
    verify(ops).increment("problem:list:version");
    when(ops.increment("problem:list:version")).thenThrow(new IllegalStateException("Redis down"));
    assertThatCode(synchronization::afterCommit).doesNotThrowAnyException();
    assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
  }

  @SuppressWarnings("unchecked")
  @Test
  void absentRedisBeanAllowsDatabaseOnlyOperation() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    TransactionSynchronizationManager.initSynchronization();
    new RedisProblemListVersionAdapter(provider).incrementAfterCommit();
    assertThatCode(
            () -> TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit())
        .doesNotThrowAnyException();
  }
}
