package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.ProblemSpecCachePort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class RedisProblemSpecCacheAdapter implements ProblemSpecCachePort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisProblemSpecCacheAdapter.class);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisProblemSpecCacheAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public void invalidateAfterCommit(UUID problemId) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              var client = redis.getIfAvailable();
              if (client != null) {
                client.delete("problem:spec:" + problemId);
              }
            } catch (RuntimeException unavailable) {
              LOG.warn("Problem specification cache invalidation unavailable");
            }
          }
        });
  }
}
