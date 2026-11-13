package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Optional 60-second storage; only the origin read may raise a business/infrastructure error. */
@Component
class CatalogReadCache {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ObjectProvider<StringRedisTemplate> redis;
  private final ObjectProvider<JdbcClient> jdbc;

  CatalogReadCache(ObjectProvider<StringRedisTemplate> redis, ObjectProvider<JdbcClient> jdbc) {
    this.redis = redis;
    this.jdbc = jdbc;
  }

  String version() {
    return jdbc.getObject()
        .sql("SELECT version FROM problem.catalog_cache_epoch WHERE id = 1")
        .query(Long.class)
        .single()
        .toString();
  }

  <T> T read(String key, Class<T> type, Supplier<T> origin) {
    try {
      var client = redis.getIfAvailable();
      if (client != null) {
        String value = client.opsForValue().get(key);
        if (value != null) {
          T decoded = JSON.readValue(value, type);
          if (valid(decoded)) {
            return decoded;
          }
        }
      }
    } catch (RuntimeException unavailable) {
      /* Cache failure falls back to the origin. */
    }
    T result = origin.get();
    if (result != null) {
      try {
        var client = redis.getIfAvailable();
        if (client != null) {
          client.opsForValue().set(key, JSON.writeValueAsString(result), Duration.ofSeconds(60));
        }
      } catch (RuntimeException unavailable) {
        /* A failed cache write never fails a read. */
      }
    }
    return result;
  }

  private static boolean valid(Object value) {
    if (value instanceof Page page) {
      return page.page() >= 1
          && page.totalItems() >= 0
          && page.items() != null
          && page.items().stream()
              .allMatch(row -> row != null && row.id() != null && row.topicNames() != null);
    }
    if (value instanceof SolveStates states) {
      return states.solved() != null
          && states.attempted() != null
          && states.solved().stream().allMatch(Objects::nonNull)
          && states.attempted().stream().allMatch(Objects::nonNull);
    }
    if (value instanceof CatalogSummary summary) {
      return summary.totalPublished() >= 0
          && summary.byTopic() != null
          && summary.byLevel() != null
          && summary.byTopic().stream().allMatch(Objects::nonNull)
          && summary.byLevel().stream().allMatch(Objects::nonNull);
    }
    return value != null;
  }
}
