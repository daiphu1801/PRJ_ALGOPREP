package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real SQL/current JWT and admin-setting endpoints; transaction time fixes boundary oracles. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class QueueMonitorFlowTest {
  private static final String BASE = "/api/v1/judge/admin/queue";

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
  @MockitoSpyBean QueueMonitorRepository repository;
  private final JsonMapper json = JsonMapper.builder().build();
  private MockMvc mvc;
  private UUID adminId;
  private String admin;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // This class owns the private container and creates no submission-dependent rows.
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update(
        "UPDATE judge.cluster_settings SET queue_paused=false,autoscale_enabled=true,autoscale_job_threshold=20,jobs_per_page=20,error_retention_hours=24,latency_cap_seconds=10");
    setRead(true);
    adminId = UUID.randomUUID();
    admin = token(adminId, BaseCategory.ADMIN);
  }

  private String token(UUID id, BaseCategory role) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Monitor user',id,'ACTIVE' FROM identity.roles WHERE code=?",
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
    var request = get(BASE + path);
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    var response = mvc.perform(request).andReturn().getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode read(String path) throws Exception {
    return read(path, admin, 200);
  }

  private void save(String path, String body) throws Exception {
    var response =
        mvc.perform(
                put(BASE + path)
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
  }

  private record Times(Instant submitted, Instant started, Instant finished) {}

  private UUID job(String status, Instant submitted, Instant started, Instant finished) {
    UUID id = UUID.randomUUID();
    insert(id, status, new Times(submitted, started, finished));
    return id;
  }

  private void insert(UUID id, String status, Times times) {
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,failure_reason,testcase_set_version_at_grading,submitted_at,started_grading_at,finished_at) VALUES(?,?,?,'JAVA','STANDARD_IO','queue-monitor-private-source','EDITOR',?,?,1,CAST(? AS timestamptz),CAST(? AS timestamptz),CAST(? AS timestamptz))",
        id,
        adminId,
        UUID.randomUUID(),
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        times.submitted().toString(),
        times.started() == null ? null : times.started().toString(),
        times.finished() == null ? null : times.finished().toString());
  }

  @FunctionalInterface
  private interface Scenario {
    void run(Instant now) throws Exception;
  }

  private void atTransactionTime(Scenario scenario) {
    var template = new TransactionTemplate(transactions);
    template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    template.executeWithoutResult(
        tx -> {
          var now =
              jdbc.queryForObject("SELECT transaction_timestamp()", OffsetDateTime.class)
                  .toInstant();
          try {
            scenario.run(now);
          } catch (Exception failure) {
            throw new IllegalStateException("Monitor scenario failed", failure);
          }
        });
  }

  private static List<String> ids(JsonNode page) {
    var result = new java.util.ArrayList<String>();
    for (var item : page.path("items")) {
      result.add(item.path("submissionId").asText());
    }
    return result;
  }

  @Test
  void emptySnapshotAndJobsUseRealSettingsWithoutMetricsOrFakeWorkers() throws Exception {
    var snapshot = read("/snapshot");
    assertThat(snapshot.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder(
            "generatedAt",
            "status",
            "pendingCount",
            "queueConsumptionPaused",
            "autoscaleEnabled",
            "autoscaleJobThreshold",
            "avgWaitMillis",
            "latencyCapSeconds",
            "workerAutoscaleMax");
    assertThat(snapshot.path("status").asText()).isEqualTo("NORMAL");
    assertThat(snapshot.path("pendingCount").asLong()).isZero();
    assertThat(snapshot.path("avgWaitMillis").isNull()).isTrue();
    assertThat(snapshot.path("workerAutoscaleMax").asInt()).isEqualTo(8);
    var jobs = read("/jobs?page=999");
    assertThat(jobs.path("items")).isEmpty();
    assertThat(jobs.path("page").asInt()).isEqualTo(1);
    assertThat(jobs.path("pageSize").asInt()).isEqualTo(20);
    assertThat(jobs.path("counts").path("all").asLong()).isZero();
  }

  @Test
  void snapshotUsesTheStartedHourWindowAndPauseWinsTheStrictThreshold() {
    atTransactionTime(
        now -> {
          for (int i = 0; i < 25; i++) {
            job("PENDING", now.minusSeconds(100 + i), null, null);
          }
          for (int delay : new int[] {2, 4, 6}) {
            Instant started = now.minusSeconds(delay == 6 ? 3600 : delay * 100);
            job("ACCEPTED", started.minusSeconds(delay), started, now.minusSeconds(1));
          }
          job(
              "ACCEPTED",
              now.minusSeconds(9000),
              now.minusSeconds(3600).minusNanos(1000),
              now.minusSeconds(2));
          job("JUDGING", now.minusSeconds(9999), now.plusNanos(1000), null);
          var snapshot = read("/snapshot");
          assertThat(snapshot.path("pendingCount").asLong()).isEqualTo(25);
          assertThat(snapshot.path("status").asText()).isEqualTo("BACKLOGGED");
          assertThat(snapshot.path("avgWaitMillis").asLong()).isEqualTo(4000);
          assertThat(Instant.parse(snapshot.path("generatedAt").asText())).isEqualTo(now);
          save(
              "/settings",
              "{\"autoscaleJobThreshold\":25,\"jobsPerPage\":2,\"errorRetentionHours\":24,\"latencyCapSeconds\":3}");
          assertThat(read("/snapshot").path("status").asText()).isEqualTo("NORMAL");
          save("/consumption", "{\"paused\":true,\"expectedPaused\":false}");
          save(
              "/settings",
              "{\"autoscaleJobThreshold\":1,\"jobsPerPage\":2,\"errorRetentionHours\":24,\"latencyCapSeconds\":3}");
          save("/autoscale", "{\"enabled\":false,\"expectedEnabled\":true}");
          var paused = read("/snapshot");
          assertThat(paused.path("status").asText()).isEqualTo("PAUSED");
          assertThat(paused.path("queueConsumptionPaused").asBoolean()).isTrue();
          assertThat(paused.path("autoscaleEnabled").asBoolean()).isFalse();
          assertThat(paused.path("latencyCapSeconds").asInt()).isEqualTo(3);
        });
  }

  @Test
  void allTabsHaveConsistentCountsAndErrorsSortLastWithoutPrivateData() {
    atTransactionTime(
        now -> {
          UUID first = job("PENDING", now.minusSeconds(30), null, null);
          UUID second = job("PENDING", now.minusSeconds(10), null, null);
          UUID compiling = job("COMPILING", now.minusSeconds(120), now.minusSeconds(20), null);
          UUID judging = job("JUDGING", now.minusSeconds(150), now.minusSeconds(5), null);
          UUID failed =
              job(
                  "SYSTEM_ERROR",
                  now.minusSeconds(10000),
                  now.minusSeconds(9000),
                  now.minusSeconds(7200));
          job("WRONG_ANSWER", now.minusSeconds(30), now.minusSeconds(20), now.minusSeconds(10));
          var waiting = read("/jobs");
          assertThat(ids(waiting)).containsExactly(first.toString(), second.toString());
          var all = read("/jobs?tab=all");
          assertThat(ids(all))
              .containsExactly(
                  first.toString(),
                  compiling.toString(),
                  second.toString(),
                  judging.toString(),
                  failed.toString());
          assertThat(all.path("counts").path("waiting").asLong()).isEqualTo(2);
          assertThat(all.path("counts").path("failed").asLong()).isEqualTo(1);
          assertThat(all.path("counts").path("all").asLong()).isEqualTo(5);
          assertThat(all.path("totalCount").asLong()).isEqualTo(5);
          long[] waits = {30000, 20000, 10000, 5000};
          for (int i = 0; i < waits.length; i++) {
            var row = all.path("items").get(i);
            assertThat(row.path("waitingMillis").asLong()).isEqualTo(waits[i]);
            assertThat(row.properties().stream().map(Map.Entry::getKey))
                .containsExactlyInAnyOrder("submissionId", "language", "status", "waitingMillis");
          }
          var errors = read("/jobs?tab=failed");
          assertThat(ids(errors)).containsExactly(failed.toString());
          assertThat(errors.path("items").get(0).path("waitingMillis").isNull()).isTrue();
          assertThat(all.toString())
              .doesNotContain(
                  "queue-monitor-private-source",
                  "sourceCode",
                  "stderr",
                  "testcase",
                  "userId",
                  "problemId",
                  "priority",
                  "jobId");
        });
  }

  @Test
  void retentionIsInclusiveAndChangingItDoesNotDeleteExpiredSubmissions() {
    atTransactionTime(
        now -> {
          UUID boundary =
              job("SYSTEM_ERROR", now.minusSeconds(90000), null, now.minusSeconds(86400));
          UUID older =
              job(
                  "SYSTEM_ERROR",
                  now.minusSeconds(90000),
                  null,
                  now.minusSeconds(86400).minusNanos(1000));
          var oldRow = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", older);
          assertThat(ids(read("/jobs?tab=failed"))).containsExactly(boundary.toString());
          assertThat(read("/jobs?tab=all").path("counts").path("failed").asLong()).isEqualTo(1);
          save(
              "/settings",
              "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":20,\"errorRetentionHours\":25,\"latencyCapSeconds\":10}");
          assertThat(ids(read("/jobs?tab=failed")))
              .containsExactlyInAnyOrder(boundary.toString(), older.toString());
          assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", older))
              .isEqualTo(oldRow);
        });
  }

  @Test
  void adminPageSizeAppliesImmediatelyAndInputPagesClampInsteadOfBypassingIt() {
    atTransactionTime(
        now -> {
          UUID first = job("PENDING", now.minusSeconds(30), null, null);
          UUID second = job("PENDING", now.minusSeconds(20), null, null);
          UUID third = job("PENDING", now.minusSeconds(10), null, null);
          assertThat(read("/jobs").path("items")).hasSize(3);
          save(
              "/settings",
              "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":2,\"errorRetentionHours\":24,\"latencyCapSeconds\":7}");
          for (String page : List.of("", "bad", "0", "-1", "2147483648")) {
            var result = read("/jobs?tab=unknown&page=" + page + "&pageSize=999");
            assertThat(ids(result)).containsExactly(first.toString(), second.toString());
            assertThat(result.path("page").asInt()).isEqualTo(1);
            assertThat(result.path("pageSize").asInt()).isEqualTo(2);
            assertThat(result.path("totalCount").asLong()).isEqualTo(3);
          }
          var last = read("/jobs?tab=all&page=2147483647");
          assertThat(last.path("page").asInt()).isEqualTo(2);
          assertThat(ids(last)).containsExactly(third.toString());
          assertThat(read("/snapshot").path("latencyCapSeconds").asInt()).isEqualTo(7);
        });
  }

  @Test
  void postgresUuidTiesAndLongWaitingTimesStayDeterministicWithoutRetentionOverflow() {
    atTransactionTime(
        now -> {
          UUID low = UUID.fromString("7fffffff-ffff-4fff-8fff-ffffffffffff");
          UUID high = UUID.fromString("80000000-0000-4000-8000-000000000000");
          insert(high, "PENDING", new Times(now.minusSeconds(3), null, null));
          insert(low, "PENDING", new Times(now.minusSeconds(3), null, null));
          assertThat(ids(read("/jobs"))).containsExactly(low.toString(), high.toString());
          Instant ancient = Instant.parse("1900-01-01T00:00:00Z");
          UUID oldPending = job("PENDING", ancient, null, null);
          UUID oldError = job("SYSTEM_ERROR", ancient, null, ancient.plusSeconds(1));
          save(
              "/settings",
              "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":2147483647,\"errorRetentionHours\":2147483647,\"latencyCapSeconds\":10}");
          var all = read("/jobs?tab=all");
          assertThat(ids(all))
              .containsExactly(
                  oldPending.toString(), low.toString(), high.toString(), oldError.toString());
          assertThat(all.path("items").get(0).path("waitingMillis").asLong())
              .isGreaterThan(Integer.MAX_VALUE);
          assertThat(all.path("counts").path("failed").asLong()).isEqualTo(1);
          assertThat(all.path("pageSize").asInt()).isEqualTo(Integer.MAX_VALUE);
        });
  }

  @Test
  void bothReadRoutesRequireCurrentReadPermissionAndNeverWriteAudit() throws Exception {
    long audits =
        jdbc.queryForObject("SELECT count(*) FROM identity.system_audit_logs", Long.class);
    for (String path : List.of("/snapshot", "/jobs")) {
      read(path, null, 401);
      for (var role : List.of(BaseCategory.STUDENT, BaseCategory.INSTRUCTOR)) {
        var bearer = token(UUID.randomUUID(), role);
        assertThat(read(path, bearer, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      }
    }
    try {
      setRead(false);
      for (String path : List.of("/snapshot", "/jobs")) {
        assertThat(read(path, admin, 403).path("errorCode").asText()).isEqualTo("JDG-301");
      }
    } finally {
      setRead(true);
    }
    read("/snapshot");
    read("/jobs");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM identity.system_audit_logs", Long.class))
        .isEqualTo(audits);
  }

  @Test
  void databaseFailuresAre503WithoutFakeEmptyDataOrLeakedSql() throws Exception {
    doThrow(new DataAccessResourceFailureException("private-sql-fault"))
        .when(repository)
        .snapshot();
    doThrow(new IllegalStateException("private-settings-fault")).when(repository).jobSettings();
    for (String path : List.of("/snapshot", "/jobs")) {
      var error = read(path, admin, 503);
      assertThat(error.toString())
          .doesNotContain(
              "private-sql-fault",
              "private-settings-fault",
              "SQL",
              "pendingCount",
              "items",
              "errorCode",
              "JDG-506",
              "JDG-507");
    }
  }
}
