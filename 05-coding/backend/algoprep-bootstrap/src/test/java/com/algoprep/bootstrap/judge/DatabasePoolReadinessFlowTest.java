package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=2",
      "spring.datasource.hikari.minimum-idle=0",
      "spring.datasource.hikari.pool-name=scale-readiness"
    })
@ActiveProfiles("test")
@Testcontainers
class DatabasePoolReadinessFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired WebApplicationContext context;
  @Autowired HikariDataSource pool;
  @Autowired Flyway flyway;
  private MockMvc mvc;

  @BeforeEach
  void http() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void exhaustedPoolReturnsFixed503WithinFiveSecondsForDetailAndProgress() throws Exception {
    assertThat(pool.getConnectionTimeout()).isEqualTo(2000);
    assertThat(flyway.getConfiguration().isOutOfOrder()).isTrue();
    String path = "/api/v1/judge/submissions/" + UUID.randomUUID();
    var caller =
        jwt()
            .jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    assertThat(mvc.perform(get(path).with(caller)).andReturn().getResponse().getStatus())
        .isEqualTo(404);
    try (Connection first = pool.getConnection();
        Connection second = pool.getConnection()) {
      assertThat(first.isClosed()).isFalse();
      assertThat(second.isClosed()).isFalse();
      for (String suffix : new String[] {"", "/progress"}) {
        long start = System.nanoTime();
        var response = mvc.perform(get(path + suffix).with(caller)).andReturn().getResponse();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
            .contains("JDG-506")
            .doesNotContain("SQL", "HikariPool", "jdbc:");
        assertThat(elapsed).isLessThan(5000);
      }
    }
  }

  @Test
  void prometheusExposesHikariPoolPressureGauges() throws Exception {
    try (Connection first = pool.getConnection();
        Connection second = pool.getConnection()) {
      assertThat(first.isClosed()).isFalse();
      assertThat(second.isClosed()).isFalse();
      var response = mvc.perform(get("/actuator/prometheus")).andReturn().getResponse();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getContentAsString())
          .contains(
              "hikaricp_connections_active",
              "hikaricp_connections_pending",
              "hikaricp_connections_max",
              "scale-readiness");
    }
  }
}
