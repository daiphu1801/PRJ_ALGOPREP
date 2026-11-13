package com.algoprep.judge.infrastructure.cache;

import com.algoprep.judge.application.ports.out.ClassSubmissionMetricsCache;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
class RedisClassSubmissionMetricsCache implements ClassSubmissionMetricsCache {
  private static final Duration TTL = Duration.ofMinutes(15);
  private final ObjectProvider<StringRedisTemplate> redis;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  RedisClassSubmissionMetricsCache(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public Optional<List<StudentSubmissionMetrics>> get(ClassMetricsScope scope, LocalDate week) {
    var client = redis.getIfAvailable();
    if (client == null) {
      return Optional.empty();
    }
    String value = client.opsForValue().get(key(scope, week));
    return value == null
        ? Optional.empty()
        : Optional.of(
            json.readValue(value, new TypeReference<List<StudentSubmissionMetrics>>() {}));
  }

  @Override
  public void put(ClassMetricsScope scope, LocalDate week, List<StudentSubmissionMetrics> metrics) {
    var client = redis.getIfAvailable();
    if (client != null) {
      client.opsForValue().set(key(scope, week), json.writeValueAsString(metrics), TTL);
    }
  }

  private String key(ClassMetricsScope scope, LocalDate week) {
    return "judge:classmetrics:"
        + scope.classId()
        + ":"
        + hash(json.writeValueAsString(scope.students()))
        + ":"
        + hash(json.writeValueAsString(scope.assignments()))
        + ":"
        + scope.weekCount()
        + ":"
        + week;
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("SHA-256 unavailable");
    }
  }
}
