package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commit/rollback of the epoch follows the mutation; Redis remains a best-effort mirror. */
@Component
@Primary
class DurableProblemListVersionAdapter implements ProblemListVersionPort {
  private final ObjectProvider<JdbcClient> jdbc;
  private final ProblemListVersionPort redis;

  DurableProblemListVersionAdapter(
      ObjectProvider<JdbcClient> jdbc,
      @Qualifier("redisProblemListVersionAdapter") ProblemListVersionPort redis) {
    this.jdbc = jdbc;
    this.redis = redis;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void incrementAfterCommit() {
    jdbc.getObject()
        .sql("UPDATE problem.catalog_cache_epoch SET version = version + 1 WHERE id = 1")
        .update();
    redis.incrementAfterCommit();
  }
}
