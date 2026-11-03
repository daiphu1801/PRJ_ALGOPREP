package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Never increments for a rolled-back change; Redis failure leaves database reads available. */
@Component
class RedisProblemListVersionAdapter implements ProblemListVersionPort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisProblemListVersionAdapter.class);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisProblemListVersionAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public void incrementAfterCommit() {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              var client = redis.getIfAvailable();
              if (client != null) {
                client.opsForValue().increment("problem:list:version");
              }
            } catch (RuntimeException unavailable) {
              LOG.warn("Problem list cache invalidation unavailable");
            }
          }
        });
  }
}
