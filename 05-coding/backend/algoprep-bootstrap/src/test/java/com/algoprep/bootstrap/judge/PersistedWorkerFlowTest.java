package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP/broker/PG/F2/F3/sandbox grading, including worker metrics and compile samples. */
@org.springframework.context.annotation.Import({
  SubmissionStompFlowTest.BrokerBarrier.class,
  PersistedWorkerFlowTest.Counting.class
})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "judge.worker.enabled=true",
      "judge.worker.gate-delay-ms=100",
      "judge.worker.scale-delay-ms=100",
      "spring.autoconfigure.exclude=",
      "spring.datasource.hikari.maximum-pool-size=18"
    })
@ActiveProfiles({"test", "worker"})
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
    named = "judge.go-judge.base-url",
    matches = "http://127.0.0.1:[0-9]+")
class PersistedWorkerFlowTest {
  @Autowired io.micrometer.prometheusmetrics.PrometheusMeterRegistry metrics;

  @Test
  void workerProfileExportsTheDocumentedPrometheusConsumerMeters() throws Exception {
    pause();
    waitUntil(() -> metrics.get("judge.grading.consumers.active").gauge().value() == 0);
    assertThat(metrics.get("judge.grading.consumers.target").gauge().value()).isBetween(1.0, 8.0);
    assertThat(metrics.get("judge.grading.consumers.active").gauge().value()).isZero();
    assertThat(metrics.scrape())
        .contains("judge_grading_consumers_target", "judge_grading_consumers_active");
  }

  @Test
  void eightQueuedJobsCompleteWithTheWorkerProfileAndConfiguredCompileBudget() throws Exception {
    pause();
    jdbc.update(
        "UPDATE judge.cluster_settings SET autoscale_enabled=true,autoscale_job_threshold=1");
    var ids = new java.util.ArrayList<UUID>();
    int before = counting.compiles.get();
    for (int i = 0; i < 8; i++) {
      String language = List.of("JAVA", "CPP", "PYTHON").get(i % 3);
      ids.add(submit(language, "FUNCTION_WRAPPER", source(language, "FUNCTION_WRAPPER")));
    }
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
    for (UUID id : ids) {
      terminal(id, "ACCEPTED");
    }
    assertThat(counting.compiles.get() - before).isEqualTo(8);
    assertThat(counting.peak.get()).isBetween(1, 8);
    System.out.println("WORKER_COMPILE_BATCH jobs=8 peakParallelCompiles=" + counting.peak.get());
  }

  @Test
  void pausedWorkerRemainsReadyAndItsLeaderCannotBeAcquiredByAnotherConnection() throws Exception {
    pause();
    assertThat(leases.acquire("F4_GRADING_WORKER_LEADER")).isEmpty();
    var response =
        http.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://localhost:" + port + "/actuator/health/readiness"))
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(
            json.readTree(response.body())
                .path("components")
                .path("gradingWorker")
                .path("status")
                .asString())
        .isEqualTo("UP");
  }

  @Autowired com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase begin;
  @Autowired com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase judging;
  @Autowired com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase runTokens;
  @Autowired com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase results;

  @Test
  void redeliveryWithPersistedResultsEndsAsWorkerTimeoutWithoutRunningAgain() throws Exception {
    pause();
    UUID id = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
    begin.begin(id, 1).orElseThrow();
    judging.beginJudging(id, 10).orElseThrow();
    UUID testcase =
        jdbc.queryForObject(
            "SELECT id FROM problem.testcases WHERE problem_id=? AND position=1",
            UUID.class,
            owner.problemId());
    var issued = runTokens.issue(id, testcase).orElseThrow();
    assertThat(
            results.record(
                new com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite(
                    id, testcase, issued.id(), 1, true, "AC", 1, 1, "1", null, false)))
        .isTrue();
    int compiles = counting.compiles.get();
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
    terminal(id, "SYSTEM_ERROR");
    assertThat(
            jdbc.queryForObject(
                "SELECT failure_reason FROM judge.submissions WHERE id=?", String.class, id))
        .isEqualTo("WORKER_TIMEOUT");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                Long.class,
                id))
        .isEqualTo(1);
    assertThat(counting.compiles.get()).isEqualTo(compiles);
  }

  @Test
  void disabledChildProcessLimitStillRunsOnTheRealEngine() throws Exception {
    jdbc.update("UPDATE judge.sandbox_settings SET child_process_limit_enabled=false");
    String source =
        "import os,time\npids=[]\nfor i in range(65):\n p=os.fork()\n if p==0:\n  time.sleep(.1);os._exit(0)\n pids.append(p)\nfor p in pids: os.waitpid(p,0)\nprint(input())\n";
    UUID id = submit("PYTHON", "STANDARD_IO", source);
    terminal(id, "ACCEPTED");
  }

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

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-queue-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-queue-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @Container
  static final GenericContainer<?> RABBIT =
      new GenericContainer<>("rabbitmq:4.2.9-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test-queue-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-queue-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-queue-user");
    properties.add("spring.rabbitmq.password", () -> "test-queue-password");
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-queue-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-queue-password");
  }

  @org.springframework.boot.test.web.server.LocalServerPort int port;
  @Autowired com.algoprep.identity.application.ports.out.AccessTokenPort tokens;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
  @Autowired JdbcTemplate jdbc;
  @Autowired com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase outboxRelay;
  @Autowired PublishTestcaseSnapshotUseCase snapshots;
  @Autowired CountingEngine counting;
  @Autowired com.algoprep.judge.domain.ports.out.WorkerLeasePort leases;
  @Autowired com.algoprep.judge.application.ports.in.SweepStaleSubmissionsUseCase sweep;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private Owner owner;

  @Autowired com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase saveSpec;

  private void saveNextSpecification() {
    UUID author =
        jdbc.queryForObject(
            "SELECT author_id FROM problem.problems WHERE id=?", UUID.class, owner.problemId());
    var body = json.createObjectNode().put("functionName", "solveNext");
    body.set("returnType", json.createObjectNode().put("kind", "INT"));
    body.set("parameters", json.readTree("[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]"));
    body.set("nameOverrides", json.createObjectNode());
    saveSpec.save(
        new com.algoprep.problembank.application.dto.SaveProblemSpecCommand(
            owner.problemId(),
            author,
            false,
            jdbc.queryForObject(
                    "SELECT updated_at FROM problem.problems WHERE id=?",
                    java.sql.Timestamp.class,
                    owner.problemId())
                .toInstant(),
            body));
    assertThat(
            jdbc.queryForObject(
                "SELECT spec_version FROM problem.problem_specs WHERE problem_id=?",
                Integer.class,
                owner.problemId()))
        .isEqualTo(2);
  }

  private tools.jackson.databind.JsonNode learnerGet(String path, UUID actor, int expected)
      throws Exception {
    var response =
        http.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token(actor))
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
    return json.readTree(response.body());
  }

  private String savedStarter(String language) throws Exception {
    saveNextSpecification();
    String source =
        learnerGet(
                "/api/v1/harness/problems/"
                    + owner.problemId()
                    + "/starter-code?language="
                    + language
                    + "&model=FUNCTION_WRAPPER",
                owner.userId(),
                200)
            .path("sourceCode")
            .asString();
    assertThat(source).contains(signature(language)).doesNotContain("echo(");
    String placeholder =
        switch (language) {
          case "JAVA" -> "return 0;";
          case "CPP" -> "return {};";
          default -> "pass";
        };
    assertThat(source).contains(placeholder);
    return source.replace(
        placeholder, language.equals("PYTHON") ? "return value" : "return value;");
  }

  private String signature(String language) {
    return switch (language) {
      case "JAVA" -> "public int solveNext(int value)";
      case "CPP" -> "int solveNext(int value)";
      default -> "def solve_next(self, value: int) -> int:";
    };
  }

  private void assertPersistedSourceAndVersion(UUID id, String source) {
    var row =
        jdbc.queryForMap(
            "SELECT source_code,spec_version,failure_reason FROM judge.submissions WHERE id=?", id);
    assertThat(row.get("source_code")).isEqualTo(source);
    assertThat(row.get("spec_version")).isEqualTo(2);
    assertThat(row.get("failure_reason")).isNull();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"JAVA", "CPP", "PYTHON"})
  void savedSpecificationAndHttpStarterReachPersistedAcceptedAndPrivateResult(String language)
      throws Exception {
    String solution = savedStarter(language);
    UUID id = submit(language, "FUNCTION_WRAPPER", solution);
    terminal(id, "ACCEPTED");
    assertPersistedSourceAndVersion(id, solution);
    var detail = learnerGet("/api/v1/judge/submissions/" + id, owner.userId(), 200);
    assertThat(detail.path("status").asString()).isEqualTo("ACCEPTED");
    assertThat(detail.path("sourceCode").asString()).isEqualTo(solution);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                Integer.class,
                id))
        .isEqualTo(10);
    learnerGet("/api/v1/judge/submissions/" + id, user("STUDENT"), 404);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "JAVA,name",
    "JAVA,arity",
    "CPP,name",
    "CPP,arity",
    "PYTHON,name",
    "PYTHON,arity"
  })
  void wrongSavedSignatureIsPersistedCompileErrorWithCurrentDeclaration(
      String language, String kind) throws Exception {
    String solution = savedStarter(language);
    String malformed =
        kind.equals("name")
            ? solution.replace("solveNext(", "other(").replace("solve_next(", "other(")
            : solution
                .replace("int value)", "int value, int unused)")
                .replace("value: int)", "value: int, unused: int)");
    assertThat(malformed).isNotEqualTo(solution);
    UUID id = submit(language, "FUNCTION_WRAPPER", malformed);
    terminal(id, "COMPILE_ERROR");
    assertFailureContract(id, "COMPILE_ERROR");
    assertPersistedSourceAndVersion(id, malformed);
    var detail = learnerGet("/api/v1/judge/submissions/" + id, owner.userId(), 200);
    assertThat(detail.path("status").asString()).isEqualTo("COMPILE_ERROR");
    String message = detail.path("compileErrorMessage").asString();
    assertThat(message)
        .contains(signature(language), "tên hàm", "tham số")
        .doesNotContain("ApHRunner", "/w/", "Traceback", "__aph", "harness.compile", "echo(");
    assertThat(
            jdbc.queryForObject(
                "SELECT compile_error_message FROM judge.submissions WHERE id=?", String.class, id))
        .isEqualTo(message);
  }

  @BeforeEach
  void fixture() throws Exception {
    configureFixture();
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    UUID actor = user("INSTRUCTOR");
    owner = new Owner(user("STUDENT"), UUID.randomUUID());
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb)"
            + " VALUES (?,?,'Queue fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        owner.problemId(),
        owner.problemId().toString(),
        actor,
        actor);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version) VALUES (?,?,'EXACT',1)",
        UUID.randomUUID(),
        owner.problemId());
    for (int i = 1; i <= 10; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline)"
              + " VALUES (?,?,?,?,true,'INLINE',?,'INLINE',?)",
          UUID.randomUUID(),
          owner.problemId(),
          i <= 2 ? "SAMPLE" : "HIDDEN",
          i,
          i + "\n",
          i + "\n");
    }
    jdbc.update(
        "UPDATE problem.problem_specs SET function_name='echo',return_type='{\"kind\":\"INT\"}'::jsonb,parameters='[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]'::jsonb WHERE problem_id=?",
        owner.problemId());
    snapshots.publishSnapshot(owner.problemId());
    jdbc.update(
        "UPDATE problem.problems SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=?",
        owner.problemId());
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare("judge.submissions", true, false, false, null);
      channel.queuePurge("judge.submissions");
    }
  }

  private void configureFixture() {
    // Functional integration cases need headroom under static-analysis/container CPU contention.
    // Production limits and timeout classification are tested separately.
    jdbc.update("UPDATE judge.grading_defaults SET compile_timeout_sec=60");
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false,autoscale_enabled=false");
    jdbc.update(
        "UPDATE judge.sandbox_settings SET network_access_enabled=false,child_process_limit_enabled=true,return_stderr_to_student=true");
    jdbc.update("UPDATE judge.language_configs SET enabled=true");
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Queue fixture',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private static ConnectionFactory factory() {
    var factory = new ConnectionFactory();
    factory.setHost(RABBIT.getHost());
    factory.setPort(RABBIT.getMappedPort(5672));
    factory.setUsername("test-queue-user");
    factory.setPassword("test-queue-password");
    factory.setAutomaticRecoveryEnabled(false);
    return factory;
  }

  private static GetResponse message() throws Exception {
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      return channel.basicGet("judge.submissions", true);
    }
  }

  @TestConfiguration
  static class Counting {
    @Bean
    @Primary
    CountingEngine countingEngine(
        @Qualifier("goJudgeAdapter")
            com.algoprep.judge.application.ports.out.JudgeExecutionPort actual) {
      return new CountingEngine(actual);
    }
  }

  static class CountingEngine
      implements com.algoprep.judge.application.ports.out.JudgeExecutionPort {
    volatile RuntimeBarrier barrier;
    final com.algoprep.judge.application.ports.out.JudgeExecutionPort actual;
    final java.util.concurrent.atomic.AtomicInteger compiling =
        new java.util.concurrent.atomic.AtomicInteger();
    final java.util.concurrent.atomic.AtomicInteger peak =
        new java.util.concurrent.atomic.AtomicInteger();
    final java.util.concurrent.atomic.AtomicInteger compiles =
        new java.util.concurrent.atomic.AtomicInteger();
    final java.util.concurrent.atomic.AtomicInteger runs =
        new java.util.concurrent.atomic.AtomicInteger();

    CountingEngine(com.algoprep.judge.application.ports.out.JudgeExecutionPort actual) {
      this.actual = actual;
    }

    private CompiledExecution measuredCompile(
        com.algoprep.judge.application.dto.ExecutionModels.CompileRequest request) {
      int active = compiling.incrementAndGet();
      peak.accumulateAndGet(active, Math::max);
      long start = System.nanoTime();
      try {
        var result = actual.compile(request);
        System.out.println(
            "WORKER_COMPILE_SAMPLE language="
                + request.program().language()
                + " model="
                + request.program().model()
                + " success="
                + result.compilation().success()
                + " wallMs="
                + java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                + " budgetMs="
                + request.limits().timeMs());
        return result;
      } finally {
        compiling.decrementAndGet();
      }
    }

    @Override
    public CompiledExecution compile(
        com.algoprep.judge.application.dto.ExecutionModels.CompileRequest request) {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      compiles.incrementAndGet();
      var currentBarrier = barrier;
      if (currentBarrier != null) {
        currentBarrier.beforeCompile();
      }
      var compiled = measuredCompile(request);
      return new CompiledExecution() {
        @Override
        public com.algoprep.judge.application.dto.ExecutionModels.Compilation compilation() {
          return compiled.compilation();
        }

        @Override
        public com.algoprep.judge.application.dto.ExecutionModels.RunResult run(
            com.algoprep.judge.application.dto.ExecutionModels.RunRequest run) {
          assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
          runs.incrementAndGet();
          if (currentBarrier != null) {
            currentBarrier.runTimes.add(run.limits().timeMs());
          }
          return compiled.run(run);
        }

        @Override
        public void close() {
          compiled.close();
        }
      };
    }
  }

  static final class RuntimeBarrier implements AutoCloseable {
    final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
    final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    final java.util.Queue<Long> runTimes = new java.util.concurrent.ConcurrentLinkedQueue<>();

    void beforeCompile() {
      entered.countDown();
      try {
        assertThat(release.await(60, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      } catch (InterruptedException failure) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Runtime acceptance barrier interrupted", failure);
      }
    }

    @Override
    public void close() {
      release.countDown();
    }
  }

  private String runtimeAdmin(UUID actor) {
    return tokens
        .issue(
            actor,
            com.algoprep.identity.domain.model.BaseCategory.ADMIN,
            "ADMIN",
            UUID.randomUUID())
        .value();
  }

  private tools.jackson.databind.JsonNode adminRequest(String admin, String path, String body)
      throws Exception {
    var request =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://localhost:" + port + "/api/v1/judge/admin/" + path))
            .header("Authorization", "Bearer " + admin);
    if (body == null) {
      request.GET();
    } else {
      request
          .header("Content-Type", "application/json")
          .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(body));
    }
    var response = http.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
    return json.readTree(response.body());
  }

  private void httpPause(String admin, boolean expected, boolean paused) throws Exception {
    assertThat(
            adminRequest(
                    admin,
                    "queue/consumption",
                    "{\"expectedPaused\":" + expected + ",\"paused\":" + paused + "}")
                .path("paused")
                .asBoolean())
        .isEqualTo(paused);
  }

  private int actorAudits(UUID actor, String action) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type=?",
        Integer.class,
        actor,
        action);
  }

  @Test
  void finalHttpPauseDrainsBusyWorkerKeepsNewJobPendingAndResumeAuditsOnce() throws Exception {
    UUID actor = user("ADMIN");
    String admin = runtimeAdmin(actor);
    int before = counting.compiles.get();
    UUID busy;
    UUID pending;
    try (var barrier = new RuntimeBarrier()) {
      counting.barrier = barrier;
      busy = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
      assertThat(barrier.entered.await(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      httpPause(admin, false, true);
      pending = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
      httpPause(admin, true, true);
      assertThat(actorAudits(actor, "QUEUE_PAUSE_CHANGED")).isEqualTo(1);
      assertThat(status(busy)).isEqualTo("COMPILING");
      assertThat(status(pending)).isEqualTo("PENDING");
    } finally {
      counting.barrier = null;
    }
    terminal(busy, "ACCEPTED");
    waitUntil(() -> metrics.get("judge.grading.consumers.active").gauge().value() == 0);
    assertThat(status(pending)).isEqualTo("PENDING");
    assertThat(counting.compiles.get() - before).isEqualTo(1);
    httpPause(admin, true, false);
    terminal(pending, "ACCEPTED");
    assertThat(actorAudits(actor, "QUEUE_PAUSE_CHANGED")).isEqualTo(2);
  }

  @Test
  void finalHttpAutoscaleResizesRealRabbitConsumersAndDisablingDrainsToOne() throws Exception {
    UUID actor = user("ADMIN");
    String admin = runtimeAdmin(actor);
    httpPause(admin, false, true);
    waitUntil(() -> metrics.get("judge.grading.consumers.active").gauge().value() == 0);
    adminRequest(
        admin,
        "queue/settings",
        "{\"autoscaleJobThreshold\":1,\"jobsPerPage\":20,\"errorRetentionHours\":24,\"latencyCapSeconds\":10}");
    adminRequest(admin, "queue/autoscale", "{\"expectedEnabled\":false,\"enabled\":true}");
    var queued = new java.util.ArrayList<UUID>();
    try (var barrier = new RuntimeBarrier()) {
      counting.barrier = barrier;
      for (int i = 0; i < 5; i++) {
        queued.add(submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO")));
      }
      httpPause(admin, true, false);
      // The container grows before the target gauge is stored, so wait for both, not just active.
      waitUntil(
          () ->
              metrics.get("judge.grading.consumers.active").gauge().value() >= 2
                  && metrics.get("judge.grading.consumers.target").gauge().value() >= 2);
      assertThat(metrics.get("judge.grading.consumers.target").gauge().value()).isBetween(2.0, 8.0);
      adminRequest(admin, "queue/autoscale", "{\"expectedEnabled\":true,\"enabled\":false}");
      assertThat(actorAudits(actor, "QUEUE_AUTOSCALE_CHANGED")).isEqualTo(2);
    } finally {
      counting.barrier = null;
    }
    for (UUID id : queued) {
      terminal(id, "ACCEPTED");
    }
    waitUntil(
        () ->
            metrics.get("judge.grading.consumers.active").gauge().value() == 1
                && metrics.get("judge.grading.consumers.target").gauge().value() == 1);
  }

  @Test
  void finalHttpLanguageUpdateKeepsBusyJobLimitsAndNextJobReadsNewConfiguration() throws Exception {
    UUID actor = user("ADMIN");
    String admin = runtimeAdmin(actor);
    var old = (tools.jackson.databind.node.ObjectNode) adminRequest(admin, "language-config", null);
    var python = pythonConfig(old);
    long previousMs = Math.round(1000 * python.path("timeLimitMultiplier").asDouble());
    int nextMultiplier = previousMs == 3000 ? 4 : 3;
    UUID busy;
    try (var barrier = new RuntimeBarrier()) {
      counting.barrier = barrier;
      busy = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
      assertThat(barrier.entered.await(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      python.put("timeLimitMultiplier", nextMultiplier);
      adminRequest(admin, "language-config", old.toString());
      barrier.close();
      terminal(busy, "ACCEPTED");
      assertThat(barrier.runTimes).hasSize(10).containsOnly(previousMs);
    } finally {
      counting.barrier = null;
    }
    try (var barrier = new RuntimeBarrier()) {
      counting.barrier = barrier;
      barrier.close();
      terminal(submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO")), "ACCEPTED");
      assertThat(barrier.runTimes).hasSize(10).containsOnly(nextMultiplier * 1000L);
    } finally {
      counting.barrier = null;
    }
    assertThat(actorAudits(actor, "LANGUAGE_CONFIG_CHANGED")).isEqualTo(1);
    restorePythonMultiplier(admin, previousMs);
  }

  private tools.jackson.databind.node.ObjectNode pythonConfig(
      tools.jackson.databind.JsonNode body) {
    return java.util.stream.StreamSupport.stream(body.path("languages").spliterator(), false)
        .filter(row -> row.path("language").asString().equals("PYTHON"))
        .map(row -> (tools.jackson.databind.node.ObjectNode) row)
        .findFirst()
        .orElseThrow();
  }

  private void restorePythonMultiplier(String admin, long previousMs) throws Exception {
    var restored = adminRequest(admin, "language-config", null);
    pythonConfig(restored).put("timeLimitMultiplier", previousMs / 1000.0);
    adminRequest(admin, "language-config", restored.toString());
  }

  @Test
  @org.junit.jupiter.api.condition.EnabledIfSystemProperty(
      named = "judge.acceptance.long-runtime",
      matches = "true")
  void finalTenMinuteEventRouteOutageKeepsLongGradingAndResultsAvailableThenReplaysOnce()
      throws Exception {
    prepareLongJob();
    long started = System.nanoTime();
    eventRoutes(false);
    var submitted = new java.util.ArrayList<UUID>();
    try {
      UUID longJob =
          submit("PYTHON", "STANDARD_IO", "import time\ntime.sleep(7)\nprint(input())\n");
      submitted.add(longJob);
      awaitLongGrading(longJob, started);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                  Integer.class,
                  longJob))
          .isEqualTo(45);
      for (int i = 0; i < 4; i++) {
        UUID id = submit("PYTHON", "STANDARD_IO", "print(0)\n");
        submitted.add(id);
        terminal(id, "WRONG_ANSWER");
      }
      awaitOutageAndReadResults(submitted, started);
      assertThat(ownerEventCount(false)).isEqualTo(6);
      assertThat(ownerEventCount(true)).isZero();
    } finally {
      eventRoutes(true);
    }
    outboxRelay.relay();
    waitUntil(
        () ->
            jdbc.queryForObject(
                    "SELECT count(*) FROM identity.processed_domain_events p JOIN judge.outbox_events o ON p.event_id=o.id JOIN judge.submissions s ON s.id=o.aggregate_id WHERE s.user_id=?",
                    Integer.class,
                    owner.userId())
                == 6);
    assertThat(ownerEventCount(true)).isEqualTo(6);
    outboxRelay.relay();
    assertThat(
            jdbc.queryForObject(
                "SELECT total_submissions FROM identity.user_submission_stats WHERE user_id=?",
                Integer.class,
                owner.userId()))
        .isEqualTo(5);
    System.out.println(
        "FINAL_OUTAGE elapsedSeconds="
            + java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)
            + " graded=5 accepted=1 replayed=6 longJobCases=45");
  }

  private void prepareLongJob() {
    jdbc.update("UPDATE problem.problems SET time_limit_ms=10000 WHERE id=?", owner.problemId());
    for (int i = 11; i <= 45; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline) VALUES (?,?,'HIDDEN',?,true,'INLINE',?,'INLINE',?)",
          UUID.randomUUID(),
          owner.problemId(),
          i,
          i + "\n",
          i + "\n");
    }
    snapshots.publishSnapshot(owner.problemId());
  }

  private void eventRoutes(boolean bind) throws Exception {
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      // Every queue that receives a judge event: with one still bound the relay sees a routable
      // message and marks it published, so the outage must cut all of them (F2 also consumes
      // graded).
      for (String[] route :
          List.of(
              new String[] {"identity.submission-graded", "judge.submission.graded"},
              new String[] {"problem-bank.submission-graded", "judge.submission.graded"},
              new String[] {"identity.submission-accepted", "judge.submission.accepted"})) {
        if (bind) {
          channel.queueBind(route[0], "algoprep.domain-events", route[1]);
        } else {
          channel.queueUnbind(route[0], "algoprep.domain-events", route[1]);
        }
      }
    }
  }

  private int ownerEventCount(boolean published) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.outbox_events o JOIN judge.submissions s ON s.id=o.aggregate_id WHERE s.user_id=?"
            + (published ? " AND o.published_at IS NOT NULL" : ""),
        Integer.class,
        owner.userId());
  }

  private void awaitLongGrading(UUID id, long started) throws Exception {
    while (List.of("PENDING", "COMPILING", "JUDGING").contains(status(id))) {
      long elapsed =
          java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started);
      assertThat(elapsed).isLessThan(420);
      sweep.sweep();
      Thread.sleep(1000);
    }
    terminal(id, "ACCEPTED");
    assertThat(java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started))
        .isGreaterThan(300);
  }

  private void awaitOutageAndReadResults(List<UUID> ids, long started) throws Exception {
    do {
      outboxRelay.relay();
      for (UUID id : ids) {
        var request =
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create(
                        "http://localhost:" + port + "/api/v1/judge/submissions/" + id))
                .header("Authorization", "Bearer " + token(owner.userId()))
                .GET()
                .build();
        var response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).path("status").asString())
            .isIn("ACCEPTED", "WRONG_ANSWER");
      }
      assertThat(ownerEventCount(true)).isZero();
      Thread.sleep(5000);
    } while (java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)
        < 600);
  }

  private String token(UUID id) {
    return tokens
        .issue(
            id,
            com.algoprep.identity.domain.model.BaseCategory.STUDENT,
            "STUDENT",
            UUID.randomUUID())
        .value();
  }

  private UUID submit(String language, String model, String source) throws Exception {
    var body =
        json.createObjectNode()
            .put("language", language)
            .put("submissionModel", model)
            .put("sourceCode", source)
            .put("sourceInputMethod", "EDITOR");
    var request =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(
                    "http://localhost:"
                        + port
                        + "/api/v1/judge/problems/"
                        + owner.problemId()
                        + "/submissions"))
            .header("Authorization", "Bearer " + token(owner.userId()))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
            .build();
    var response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);
    return UUID.fromString(json.readTree(response.body()).path("submissionId").asString());
  }

  private void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(90);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("Timed out waiting for grading state");
      }
      Thread.sleep(25);
    }
  }

  private String status(UUID id) {
    return jdbc.queryForObject("SELECT status FROM judge.submissions WHERE id=?", String.class, id);
  }

  private void terminal(UUID id, String expected) throws Exception {
    waitUntil(() -> !List.of("PENDING", "COMPILING", "JUDGING").contains(status(id)));
    assertThat(status(id)).isEqualTo(expected);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=? AND event_type='SubmissionGraded'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  private String source(String language, String model) {
    if (model.equals("FUNCTION_WRAPPER")) {
      return switch (language) {
        case "JAVA" -> "class Solution { public int echo(int value) { return value; } }";
        case "CPP" -> "class Solution { public: int echo(int value) { return value; } };";
        default -> "class Solution:\n def echo(self,value):\n  return value\n";
      };
    }
    return switch (language) {
      case "JAVA" ->
          "class Main { public static void main(String[] args) { java.util.Scanner s=new java.util.Scanner(System.in); System.out.println(s.nextInt()); } }";
      case "CPP" ->
          "#include <iostream>\nint main() { int n; std::cin>>n; std::cout<<n<<std::endl; }";
      default -> "print(input())\n";
    };
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "JAVA,STANDARD_IO",
    "CPP,STANDARD_IO",
    "PYTHON,STANDARD_IO",
    "JAVA,FUNCTION_WRAPPER",
    "CPP,FUNCTION_WRAPPER",
    "PYTHON,FUNCTION_WRAPPER"
  })
  void realQueueF2F3AndSandboxGradeBothModelsInEveryLanguage(String language, String model)
      throws Exception {
    int compiles = counting.compiles.get();
    int runs = counting.runs.get();
    UUID id = submit(language, model, source(language, model));
    terminal(id, "ACCEPTED");
    assertRelayedToIdentity(id);
    assertThat(counting.compiles.get() - compiles).isEqualTo(1);
    assertThat(counting.runs.get() - runs).isEqualTo(10);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                Long.class,
                id))
        .isEqualTo(10);
    assertThat(
            jdbc.queryForObject(
                "SELECT score_ratio FROM judge.submissions WHERE id=?",
                java.math.BigDecimal.class,
                id))
        .isEqualByComparingTo("1.000");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=? AND event_type='SubmissionAccepted'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  private void assertRelayedToIdentity(UUID id) throws Exception {
    outboxRelay.relay();
    waitUntil(
        () ->
            jdbc.queryForObject(
                    "SELECT count(*) FROM identity.processed_domain_events p JOIN judge.outbox_events o ON p.event_id=o.id WHERE o.aggregate_id=?",
                    Integer.class,
                    id)
                == 2);
    assertThat(
            jdbc.queryForObject(
                "SELECT total_submissions FROM identity.user_submission_stats WHERE user_id=?",
                Integer.class,
                owner.userId()))
        .isEqualTo(1);
  }

  @Test
  void wrongAnswerDoesNotStopLaterTestcasesAndStompKeepsItsPrivateOrder() throws Exception {
    try (var wire = StompWireClient.open(port);
        var other = StompWireClient.open(port)) {
      wire.connect(token(owner.userId()));
      assertThat(wire.next()).startsWith("CONNECTED");
      other.connect(token(user("STUDENT")));
      assertThat(other.next()).startsWith("CONNECTED");
      wire.subscribed("/user/queue/submissions");
      other.subscribed("/user/queue/submissions");
      UUID id = submit("PYTHON", "STANDARD_IO", "n=int(input())\nprint(n if n!=2 else -1)\n");
      terminal(id, "WRONG_ANSWER");
      for (int seq = 1; seq <= 13; seq++) {
        String frame = wire.next();
        var event = json.readTree(frame.substring(frame.indexOf("\n\n") + 2));
        assertThat(event.path("seq").asInt()).isEqualTo(seq);
        assertThat(event.path("submissionId").asString()).isEqualTo(id.toString());
        assertThat(frame).doesNotContain("sourceCode", "input_inline", "expectedOutput", "stderr");
      }
      other.noMessage();
      assertThat(
              jdbc.queryForObject(
                  "SELECT passed_testcase_count FROM judge.submissions WHERE id=?",
                  Integer.class,
                  id))
          .isEqualTo(9);
      assertThat(
              jdbc.queryForObject(
                  "SELECT score_ratio FROM judge.submissions WHERE id=?",
                  java.math.BigDecimal.class,
                  id))
          .isEqualByComparingTo("0.900");
    }
  }

  private void pause() throws Exception {
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
    try (var c = factory().newConnection();
        var channel = c.createChannel()) {
      waitUntil(
          () -> {
            try {
              return channel.queueDeclarePassive("judge.submissions").getConsumerCount() == 0;
            } catch (java.io.IOException e) {
              throw new IllegalStateException(e);
            }
          });
    }
  }

  @Test
  void pausedJobUsesItsPinnedSnapshotEvenAfterCurrentTestcasesChange() throws Exception {
    pause();
    UUID id = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
    assertThat(status(id)).isEqualTo("PENDING");
    jdbc.update(
        "UPDATE problem.testcases SET expected_output_inline='changed' WHERE problem_id=?",
        owner.problemId());
    jdbc.update(
        "UPDATE problem.problems SET current_testcase_set_version=2 WHERE id=?", owner.problemId());
    snapshots.publishSnapshot(owner.problemId());
    // Disabled languages affect admission only; queued jobs still run with the newly read
    // multipliers.
    jdbc.update(
        "UPDATE judge.language_configs SET enabled=false,time_limit_multiplier=3.0 WHERE language='PYTHON'");
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
    terminal(id, "ACCEPTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT testcase_set_version_at_grading FROM judge.submissions WHERE id=?",
                Integer.class,
                id))
        .isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"compile", "signature", "input", "network", "runtime", "timeout"})
  void realFailuresHaveTheCorrectPersistedTerminalContract(String kind) throws Exception {
    String model =
        kind.equals("signature") || kind.equals("input") ? "FUNCTION_WRAPPER" : "STANDARD_IO";
    String source = source("PYTHON", model);
    String expected =
        switch (kind) {
          case "compile", "signature" -> "COMPILE_ERROR";
          case "runtime" -> "RUNTIME_ERROR";
          case "timeout" -> "TIME_LIMIT_EXCEEDED";
          default -> "SYSTEM_ERROR";
        };
    if (kind.equals("compile")) {
      source = "x=1\nprint(x)\nif:\n";
    }
    if (kind.equals("signature")) {
      source = "class Solution:\n def other(self,value):\n  return value\n";
    }
    if (kind.equals("runtime")) {
      source = "raise ValueError('private runtime')\n";
    }
    if (kind.equals("timeout")) {
      jdbc.update("UPDATE problem.problems SET time_limit_ms=100 WHERE id=?", owner.problemId());
      source = "while True: pass\n";
    }
    if (kind.equals("network")) {
      jdbc.update("UPDATE judge.sandbox_settings SET network_access_enabled=true");
    }
    if (kind.equals("input")) {
      jdbc.update(
          "UPDATE problem.testcases SET input_inline='invalid' WHERE problem_id=?",
          owner.problemId());
      jdbc.update(
          "UPDATE problem.problems SET current_testcase_set_version=2 WHERE id=?",
          owner.problemId());
      snapshots.publishSnapshot(owner.problemId());
    }
    UUID id = submit("PYTHON", model, source);
    terminal(id, expected);
    assertFailureContract(id, expected);
  }

  private void assertFailureContract(UUID id, String expected) {
    var envelope =
        json.readTree(
            jdbc.queryForObject(
                "SELECT payload::text FROM judge.outbox_events WHERE aggregate_id=? AND event_type='SubmissionGraded'",
                String.class,
                id));
    if (List.of("COMPILE_ERROR", "SYSTEM_ERROR").contains(expected)) {
      assertThat(envelope.path("payload").path("passedCount").isNull()).isTrue();
      assertThat(envelope.path("payload").path("totalCount").isNull()).isTrue();
      assertThat(envelope.path("payload").path("passedTestcaseRatio").isNull()).isTrue();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                  Long.class,
                  id))
          .isZero();
    } else {
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                  Long.class,
                  id))
          .isEqualTo(10);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=? AND NOT is_sample AND stderr_snippet IS NOT NULL",
                  Long.class,
                  id))
          .isZero();
    }
  }

  @Test
  void duplicateTerminalDeliveryAndConcurrentJobLeaseDoNotGradeAgain() throws Exception {
    var held = leases.acquire("judge:sub:" + UUID.randomUUID()).orElseThrow();
    held.close();
    UUID id = submit("PYTHON", "STANDARD_IO", source("PYTHON", "STANDARD_IO"));
    terminal(id, "ACCEPTED");
    int count = counting.compiles.get();
    try (var c = factory().newConnection();
        var channel = c.createChannel()) {
      for (int i = 0; i < 2; i++) {
        channel.basicPublish(
            "",
            "judge.submissions",
            null,
            ("{\"submissionId\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8));
      }
      waitUntil(
          () -> {
            try {
              return channel.queueDeclarePassive("judge.submissions").getMessageCount() == 0;
            } catch (java.io.IOException e) {
              throw new IllegalStateException(e);
            }
          });
    }
    assertThat(counting.compiles.get()).isEqualTo(count);
    try (var lease = leases.acquire("judge:sub:" + id).orElseThrow()) {
      assertThat(leases.acquire("judge:sub:" + id)).isEmpty();
      lease.check();
    }
    try (var lease = leases.acquire("judge:sub:" + id).orElseThrow()) {
      lease.check();
    }
  }
}
