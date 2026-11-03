package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.domain.ports.out.InfraEventRepository;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real SQL and current JWT authorities; exact boundary oracles use DB transaction time. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class InfraEventsFlowTest {
  private static final String SUMMARY = "/queue/infra-summary";
  private static final String EVENTS = "/infra-events";

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
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired WebApplicationContext context;
  @Autowired PlatformTransactionManager transactions;
  @MockitoSpyBean InfraEventRepository repository;
  private final JsonMapper json = JsonMapper.builder().build();
  private MockMvc mvc;
  private UUID adminId;
  private String admin;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM judge.outbox_events");
    jdbc.update("DELETE FROM judge.submissions");
    setRead(true);
    adminId = UUID.randomUUID();
    admin = token(adminId, BaseCategory.ADMIN);
  }

  private String token(UUID id, BaseCategory role) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Infra user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role.name());
    return tokens.issue(id, role, role.name(), UUID.randomUUID()).value();
  }

  private void setRead(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='JUDGE_QUEUE_MONITOR' AND a.code='READ'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
  }

  private JsonNode read(String path, String bearer, int expected) throws Exception {
    var request = get(URI.create("/api/v1/judge/admin" + path));
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    var response = mvc.perform(request).andReturn().getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    if (expected >= 400 && expected != 401) {
      assertThat(response.getContentType()).startsWith("application/problem+json");
    }
    return json.readTree(response.getContentAsString());
  }

  private JsonNode read(String path) throws Exception {
    return read(path, admin, 200);
  }

  private UUID outbox(int attempts, Instant at) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,publish_attempts,created_at) VALUES(?,'Submission',?,'SubmissionGraded','{\"private\":\"HIDDEN_SECRET\"}'::jsonb,?,CAST(? AS timestamptz))",
        id,
        id,
        attempts,
        at.toString());
    return id;
  }

  private UUID failure(String reason, Instant at) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,failure_reason,testcase_set_version_at_grading,submitted_at,finished_at) VALUES(?,?,?,'JAVA','STANDARD_IO','STUDENT_SECRET','EDITOR','SYSTEM_ERROR',?,1,CAST(? AS timestamptz)-interval '1 hour',CAST(? AS timestamptz))",
        id,
        adminId,
        UUID.randomUUID(),
        reason,
        at.toString(),
        at.toString());
    return id;
  }

  @FunctionalInterface
  private interface Scenario {
    void run(Instant now) throws Exception;
  }

  private void atTransactionTime(Scenario scenario) {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx -> {
              var now =
                  jdbc.queryForObject("SELECT transaction_timestamp()", OffsetDateTime.class)
                      .toInstant();
              try {
                scenario.run(now);
              } catch (Exception failure) {
                throw new IllegalStateException("Infrastructure event scenario failed", failure);
              }
            });
  }

  @Test
  void emptyResponsesHaveExactShapesAndNoFakeData() throws Exception {
    var summary = read(SUMMARY);
    assertThat(summary.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder("warnCount", "errorCount", "windowHours");
    assertThat(summary.path("warnCount").asLong()).isZero();
    assertThat(summary.path("errorCount").asLong()).isZero();
    assertThat(summary.path("windowHours").asInt()).isEqualTo(24);
    var page = read(EVENTS);
    assertThat(page.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder("events", "nextCursor", "services");
    assertThat(page.path("events")).isEmpty();
    assertThat(page.path("nextCursor").isNull()).isTrue();
    assertThat(page.path("services").toString()).isEqualTo("[\"go-judge\",\"outbox\"]");
  }

  @Test
  void bothSourcesRespectAttemptLevelsAndNeverExposePrivateColumns() {
    atTransactionTime(
        now -> {
          outbox(2, now);
          outbox(3, now);
          outbox(4, now);
          outbox(5, now);
          for (String reason :
              List.of("WORKER_TIMEOUT", "SANDBOX_FAULT", "QUEUE_UNAVAILABLE", "SPEC_UNAVAILABLE")) {
            failure(reason, now);
          }
          var response = read(EVENTS);
          assertThat(response.path("events")).hasSize(7);
          assertThat(response.toString())
              .doesNotContain(
                  "STUDENT_SECRET",
                  "HIDDEN_SECRET",
                  "payload",
                  "sourceCode",
                  "failureReason",
                  "sourceKey");
          for (var event : response.path("events")) {
            assertThat(event.properties().stream().map(Map.Entry::getKey))
                .containsExactlyInAnyOrder("occurredAt", "level", "message", "service");
          }
          assertThat(read(EVENTS + "?service=outbox&level=WARN").path("events")).hasSize(2);
          assertThat(read(EVENTS + "?service=outbox&level=ERROR").path("events")).hasSize(1);
          assertThat(read(EVENTS + "?level=info").path("events")).isEmpty();
          assertThat(read(EVENTS + "?level=bogus&service=bogus").path("events")).hasSize(7);
        });
  }

  @Test
  void summaryAndListShareInclusiveWindowsAndExcludeFutureAndExpiredRows() {
    atTransactionTime(
        now -> {
          outbox(3, now.minus(24, ChronoUnit.HOURS));
          outbox(5, now.minus(24, ChronoUnit.HOURS));
          failure("SANDBOX_FAULT", now);
          failure("WORKER_TIMEOUT", now.minus(24, ChronoUnit.HOURS).minusNanos(1000));
          outbox(5, now.minus(90, ChronoUnit.DAYS));
          outbox(5, now.minus(90, ChronoUnit.DAYS).minusNanos(1000));
          failure("SANDBOX_FAULT", now.plusSeconds(1));
          var summary = read(SUMMARY);
          assertThat(summary.path("warnCount").asLong()).isEqualTo(1);
          assertThat(summary.path("errorCount").asLong()).isEqualTo(2);
          var events = read(EVENTS).path("events");
          assertThat(events).hasSize(5);
          long recentErrors = 0;
          for (var event : events) {
            if (!Instant.parse(event.path("occurredAt").asText())
                    .isBefore(now.minus(24, ChronoUnit.HOURS))
                && event.path("level").asText().equals("ERROR")) {
              recentErrors++;
            }
          }
          assertThat(recentErrors).isEqualTo(summary.path("errorCount").asLong());
          assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.outbox_events", Long.class))
              .isEqualTo(4);
        });
  }

  @Test
  void cursorPagesHaveNoDuplicatesAcrossSourcesAndTimestampTies() {
    atTransactionTime(
        now -> {
          for (int i = 0; i < 12; i++) {
            outbox(3, now.minusNanos(123000));
            failure("SANDBOX_FAULT", now.minusNanos(123000));
          }
          var first = read(EVENTS);
          assertThat(first.path("events")).hasSize(20);
          assertThat(first.path("nextCursor").isTextual()).isTrue();
          var second = read(EVENTS + "?cursor=" + first.path("nextCursor").asText());
          assertThat(second.path("events")).hasSize(4);
          assertThat(second.path("nextCursor").isNull()).isTrue();
          var messages = new ArrayList<String>();
          for (var page : List.of(first, second)) {
            for (var event : page.path("events")) {
              messages.add(event.path("message").asText());
            }
          }
          assertThat(messages).doesNotHaveDuplicates().hasSize(24);
          for (int i = 0; i < 12; i++) {
            assertThat(first.path("events").get(i).path("service").asText()).isEqualTo("go-judge");
          }
        });
  }

  @Test
  void keywordSearchMatchesDisplayedTemplatesAndTreatsSqlWildcardsAsData() {
    atTransactionTime(
        now -> {
          UUID outbox = outbox(3, now);
          failure("SANDBOX_FAULT", now);
          failure("WORKER_TIMEOUT", now);
          failure("SPEC_UNAVAILABLE", now);
          assertThat(read(EVENTS + "?q=GO-JUDGE&service=go-judge&level=error").path("events"))
              .hasSize(1);
          assertThat(read(EVENTS + "?q=" + outbox).path("events")).hasSize(1);
          assertThat(read(EVENTS + "?q=SubmissionGraded").path("events")).hasSize(1);
          assertThat(read(EVENTS + "?q=3%20lần").path("events")).hasSize(1);
          assertThat(read(EVENTS + "?q=testcase").path("events")).hasSize(1);
          for (String q : List.of("%25", "_", "'%20OR%201=1--", "HIDDEN_SECRET")) {
            assertThat(read(EVENTS + "?q=" + q).path("events")).isEmpty();
          }
          assertThat(read(EVENTS + "?q=" + "x".repeat(101)).path("events")).isEmpty();
        });
  }

  @Test
  void dateRangesUseClosedUtcPlusSevenDaysAndInvalidInputIsGeneric400() throws Exception {
    Instant start =
        Instant.now()
            .minus(2, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.DAYS)
            .minus(7, ChronoUnit.HOURS);
    failure("SANDBOX_FAULT", start.minusNanos(1000));
    failure("SANDBOX_FAULT", start);
    failure("SANDBOX_FAULT", start.plus(1, ChronoUnit.DAYS).minusNanos(1000));
    failure("SANDBOX_FAULT", start.plus(1, ChronoUnit.DAYS));
    String day = start.plus(7, ChronoUnit.HOURS).toString().substring(0, 10);
    assertThat(read(EVENTS + "?from=" + day + "&to=" + day).path("events")).hasSize(2);
    assertThat(read(EVENTS + "?from=0001-01-01&to=0001-01-01").path("events")).isEmpty();
    assertThat(read(EVENTS + "?from=9999-12-31&to=9999-12-31").path("events")).isEmpty();
    for (String filter :
        List.of(
            "from=2026-11-02&to=2026-11-01",
            "from=2026-02-30",
            "to=bad",
            "cursor=!",
            "cursor=eA",
            "cursor=" + "a".repeat(181))) {
      var problem = read(EVENTS + "?" + filter, admin, 400);
      assertThat(problem.path("errorCode").asText()).isEqualTo("JDG-V13");
      assertThat(problem.toString()).doesNotContain("SELECT", "Exception", "source_code");
    }
  }

  @Test
  void bothRoutesRequireTheirCurrentReadCellAndNeverWriteAudit() throws Exception {
    long before =
        jdbc.queryForObject("SELECT count(*) FROM identity.system_audit_logs", Long.class);
    String student = token(UUID.randomUUID(), BaseCategory.STUDENT);
    for (String path : List.of(SUMMARY, EVENTS)) {
      read(path, null, 401);
      assertThat(read(path, student, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      read(path);
    }
    setRead(false);
    try {
      for (String path : List.of(SUMMARY, EVENTS)) {
        assertThat(read(path, admin, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      }
    } finally {
      setRead(true);
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM identity.system_audit_logs", Long.class))
        .isEqualTo(before);
  }

  @Test
  void databaseFailuresReturn503WithoutInventingEmptyDataOrLeakingSql() throws Exception {
    var fault = new DataAccessResourceFailureException("SELECT source_code STUDENT_SECRET");
    doThrow(fault).when(repository).summary();
    doThrow(fault).when(repository).list(any(), any());
    for (String path : List.of(SUMMARY, EVENTS)) {
      var response = read(path, admin, 503);
      assertThat(response.toString())
          .doesNotContain("STUDENT_SECRET", "SELECT", "\"events\"", "\"warnCount\"");
      assertThat(response.path("detail").asText())
          .isEqualTo("Infrastructure events temporarily unavailable");
    }
  }
}
