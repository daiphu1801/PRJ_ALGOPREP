package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Real F2 port -> bootstrap -> F4 -> PostgreSQL, including fresh reads and failure paths. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@Transactional
class JudgeGradingDefaultsFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JudgeConfigReadPort config;
  @Autowired JdbcTemplate jdbc;

  @Test
  void readsSeededDefaultsThroughCallerOwnedPort() {
    assertThat(config.getGradingDefaults()).isEqualTo(new JudgeGradingDefaults(1000, 256, 64, 10));
  }

  @Test
  void nextCallObservesAllFourEditedValuesWithoutCacheOrUnitConversion() {
    config.getGradingDefaults();
    jdbc.update(
        "UPDATE judge.grading_defaults SET time_limit_ms=1234, memory_limit_mb=345,"
            + " output_limit_kb=67, compile_timeout_sec=18 WHERE id=1");
    assertThat(config.getGradingDefaults()).isEqualTo(new JudgeGradingDefaults(1234, 345, 67, 18));
  }

  @Test
  void missingSingletonIsAnErrorForCallerToHandle() {
    jdbc.update("DELETE FROM judge.grading_defaults WHERE id=1");
    assertThatThrownBy(config::getGradingDefaults)
        .isInstanceOf(EmptyResultDataAccessException.class);
  }

  @Test
  void databaseFailureIsNotConvertedIntoSuccessfulDefaults() {
    jdbc.execute("ALTER TABLE judge.grading_defaults RENAME TO unavailable_defaults");
    assertThatThrownBy(config::getGradingDefaults).isInstanceOf(DataAccessException.class);
  }
}
