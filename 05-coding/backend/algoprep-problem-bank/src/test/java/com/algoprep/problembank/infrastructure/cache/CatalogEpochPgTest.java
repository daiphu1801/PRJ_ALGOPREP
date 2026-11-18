package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.infrastructure.persistence.PostgresTestBase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** The durable catalogue cache epoch against the real single-row table (V2.003). */
class CatalogEpochPgTest extends PostgresTestBase {
  private final AtomicInteger mirrored = new AtomicInteger();
  private final ProblemListVersionPort redis = mirrored::incrementAndGet;
  private final DurableProblemListVersionAdapter adapter =
      new DurableProblemListVersionAdapter(provider(), redis);
  private final CatalogReadCache cache = new CatalogReadCache(noRedis(), provider());

  @SuppressWarnings("unchecked")
  private static ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> noRedis() {
    return new ObjectProvider<>() {
      @Override
      public org.springframework.data.redis.core.StringRedisTemplate getObject() {
        return null;
      }
    };
  }

  private long epoch() {
    return jdbc.sql("SELECT version FROM problem.catalog_cache_epoch WHERE id = 1")
        .query(Long.class)
        .single();
  }

  @Test
  void incrementBumpsTheSingleRowAndTheCacheKeyFollows() {
    long before = epoch();
    assertThat(cache.version()).isEqualTo(Long.toString(before));

    tx.executeWithoutResult(status -> adapter.incrementAfterCommit());
    tx.executeWithoutResult(status -> adapter.incrementAfterCommit());

    assertThat(epoch()).isEqualTo(before + 2);
    assertThat(cache.version()).isEqualTo(Long.toString(before + 2));
    assertThat(mirrored).hasValue(2);
    assertThat(count("catalog_cache_epoch")).isEqualTo(1);
  }

  @Test
  void rolledBackMutationLeavesTheEpochUnchanged() {
    long before = epoch();

    tx.executeWithoutResult(
        status -> {
          adapter.incrementAfterCommit();
          status.setRollbackOnly();
        });

    assertThat(epoch()).isEqualTo(before);
  }

  @Test
  void tableRefusesASecondRowAndAnyIdOtherThanOne() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.sql("INSERT INTO problem.catalog_cache_epoch(id, version) VALUES (2, 0)")
                    .update())
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.sql("INSERT INTO problem.catalog_cache_epoch(id, version) VALUES (1, 5)")
                    .update())
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}
