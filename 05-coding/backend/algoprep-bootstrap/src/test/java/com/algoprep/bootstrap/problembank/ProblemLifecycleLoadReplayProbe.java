package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.application.ports.in.RelayProblemOutboxUseCase;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit operator probe: only the isolated algoprep_load DB/vhost; no migrations or message ack.
 */
@EnabledIfSystemProperty(named = "problem.lifecycle.load-replay", matches = "true")
class ProblemLifecycleLoadReplayProbe {
  private static final String QUEUE = "problem-bank.lifecycle-pending";

  @Test
  void replayOnlyTheExistingIsolatedLifecycleBacklogThroughProductionRelay() throws Exception {
    var settings = settings();
    var source = source(settings);
    var jdbc = new JdbcTemplate(source);
    assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
        .isEqualTo("algoprep_load");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.outbox_events WHERE published_at IS NULL"
                    + " AND routing_key NOT IN ('problem.problem.published','problem.problem.unpublished')",
                Integer.class))
        .isZero();
    int before = pending(jdbc);
    assertThat(before).isBetween(0, 100);
    int queuedBefore = queued(settings);
    try (var context = context(source, settings)) {
      int attempted = context.getBean(RelayProblemOutboxUseCase.class).relay();
      assertThat(attempted).isEqualTo(before);
    }
    assertThat(pending(jdbc)).isZero();
    int queuedAfter = queued(settings);
    assertThat(queuedAfter).isEqualTo(queuedBefore + before);
    System.out.printf(
        "LOAD_LIFECYCLE_REPLAY attempted=%d pendingAfter=0 queueBefore=%d queueAfter=%d%n",
        before, queuedBefore, queuedAfter);
  }

  private int pending(JdbcTemplate jdbc) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM problem.outbox_events WHERE published_at IS NULL", Integer.class);
  }

  private DriverManagerDataSource source(Map<String, Object> settings) {
    var source = new DriverManagerDataSource();
    source.setDriverClassName("org.postgresql.Driver");
    source.setUrl("jdbc:postgresql://127.0.0.1:5433/algoprep_load");
    source.setUsername((String) settings.get("ALGOPREP_DB_USER"));
    source.setPassword((String) settings.get("ALGOPREP_DB_PASSWORD"));
    return source;
  }

  private AnnotationConfigApplicationContext context(
      DriverManagerDataSource source, Map<String, Object> settings) throws Exception {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("load", settings));
    context.registerBean(JdbcClient.class, () -> JdbcClient.create(source));
    context.registerBean(
        PlatformTransactionManager.class, () -> new JdbcTransactionManager(source));
    context.register(
        Transactions.class,
        Class.forName(
            "com.algoprep.problembank.infrastructure.persistence.JdbcProblemOutboxRepository"),
        Class.forName(
            "com.algoprep.problembank.infrastructure.messaging.RabbitProblemDomainEventAdapter"),
        Class.forName(
            "com.algoprep.problembank.application.command.RelayProblemOutboxCommandHandler"));
    context.refresh();
    return context;
  }

  private int queued(Map<String, Object> settings) throws Exception {
    var factory = new ConnectionFactory();
    factory.setHost("127.0.0.1");
    factory.setPort(5672);
    factory.setVirtualHost("algoprep_followup_load");
    factory.setUsername((String) settings.get("spring.rabbitmq.username"));
    factory.setPassword((String) settings.get("spring.rabbitmq.password"));
    try (var connection = factory.newConnection();
        var channel = connection.createChannel()) {
      return channel.queueDeclarePassive(QUEUE).getMessageCount();
    }
  }

  private Map<String, Object> settings() throws Exception {
    var settings = new HashMap<String, Object>();
    for (String line : Files.readAllLines(Path.of("../../.env"))) {
      String[] parts = line.split("=", 2);
      if (parts.length == 2 && !parts[0].strip().startsWith("#")) {
        settings.put(parts[0].strip(), parts[1].strip().replaceAll("^[\"']|[\"']$", ""));
      }
    }
    settings.put("spring.rabbitmq.host", "127.0.0.1");
    settings.put("spring.rabbitmq.port", "5672");
    settings.put("spring.rabbitmq.virtual-host", "algoprep_followup_load");
    settings.put("spring.rabbitmq.username", settings.get("ALGOPREP_RABBITMQ_USER"));
    settings.put("spring.rabbitmq.password", settings.get("ALGOPREP_RABBITMQ_PASSWORD"));
    return settings;
  }

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  static class Transactions {}
}
