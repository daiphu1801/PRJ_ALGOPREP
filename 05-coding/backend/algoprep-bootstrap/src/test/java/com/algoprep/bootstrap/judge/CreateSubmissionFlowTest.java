package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/**
 * Real broker/PG/F2 admission; the test wrapper can model a lost confirmation after actual
 * delivery.
 */
@org.springframework.context.annotation.Import(SubmissionStompFlowTest.BrokerBarrier.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=8"
    })
@ActiveProfiles("test")
@Testcontainers
class CreateSubmissionFlowTest {
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
  @Autowired PublishTestcaseSnapshotUseCase snapshots;
  @Autowired ControlledQueue controlled;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private Owner owner;

  @TestConfiguration
  static class FaultConfiguration {
    @Bean
    @Primary
    ControlledQueue controlledQueue(
        @Qualifier("rabbitSubmissionQueueAdapter") SubmissionQueuePort actual, JdbcTemplate jdbc) {
      return new ControlledQueue(actual, jdbc);
    }
  }

  static class ControlledQueue implements SubmissionQueuePort {
    private final SubmissionQueuePort actual;
    private final JdbcTemplate jdbc;
    String mode = "NORMAL";

    ControlledQueue(SubmissionQueuePort actual, JdbcTemplate jdbc) {
      this.actual = actual;
      this.jdbc = jdbc;
    }

    @Override
    public boolean enqueue(UUID id) {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      if (!mode.equals("BOUNDARY")) {
        assertThat(
                jdbc.queryForObject(
                    "SELECT source_code FROM judge.submissions WHERE id=?", String.class, id))
            .isEqualTo("print('private source')\r\n");
      }
      boolean confirmed = actual.enqueue(id);
      if (mode.equals("WORKER")) {
        jdbc.update(
            "UPDATE judge.submissions SET status='COMPILING',started_grading_at=clock_timestamp() WHERE id=?",
            id);
      }
      return (mode.equals("NORMAL") || mode.equals("BOUNDARY")) && confirmed;
    }
  }

  @BeforeEach
  void fixture() throws Exception {
    controlled.mode = "NORMAL";
    jdbc.update("UPDATE judge.language_configs SET enabled=true");
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
              + " VALUES (?,?,?,?,true,'INLINE','1','INLINE','2')",
          UUID.randomUUID(),
          owner.problemId(),
          i <= 2 ? "SAMPLE" : "HIDDEN",
          i);
    }
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

  private String token(UUID id, String role) {
    return tokens
        .issue(
            id,
            com.algoprep.identity.domain.model.BaseCategory.valueOf(role),
            role,
            UUID.randomUUID())
        .value();
  }

  private tools.jackson.databind.node.ObjectNode request() {
    return json.createObjectNode()
        .put("language", "python")
        .put("submissionModel", "STANDARD_IO")
        .put("sourceCode", "print('private source')\r\n")
        .put("sourceInputMethod", "EDITOR");
  }

  private java.net.http.HttpResponse<String> submit(String token, String problemId, String body)
      throws Exception {
    var builder =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(
                    "http://localhost:"
                        + port
                        + "/api/v1/judge/problems/"
                        + problemId
                        + "/submissions"))
            .timeout(java.time.Duration.ofSeconds(25))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return http.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
  }

  private java.net.http.HttpResponse<String> submit(tools.jackson.databind.JsonNode body)
      throws Exception {
    return submit(
        token(owner.userId(), "STUDENT"),
        owner.problemId().toString(),
        json.writeValueAsString(body));
  }

  private long count() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.submissions WHERE problem_id=?", Long.class, owner.problemId());
  }

  private void error(java.net.http.HttpResponse<String> response, int status, String code) {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(response.headers().firstValue("content-type").orElse(""))
        .startsWith("application/problem+json");
    var body = json.readTree(response.body());
    assertThat(body.path("errorCode").asString()).isEqualTo(code);
    assertThat(body.path("status").asInt()).isEqualTo(status);
    assertThat(response.body())
        .doesNotContain("private source", "source_code", "Exception", "SELECT ", "Bearer ");
  }

  @Test
  void realHttpAdmissionCommitsExactBytesAndPublishesOnlyIdWithoutTrustingBodyOwner()
      throws Exception {
    var body = request().put("userId", UUID.randomUUID().toString());
    var response = submit(body);
    assertThat(response.statusCode()).isEqualTo(201);
    var ticket = json.readTree(response.body());
    assertThat(ticket.propertyNames())
        .containsExactlyInAnyOrder(
            "submissionId",
            "status",
            "language",
            "submissionModel",
            "submittedAt",
            "totalCountHint");
    assertThat(ticket.path("status").asString()).isEqualTo("PENDING");
    assertThat(ticket.path("language").asString()).isEqualTo("PYTHON");
    assertThat(ticket.path("totalCountHint").asInt()).isEqualTo(10);
    UUID id = UUID.fromString(ticket.path("submissionId").asString());
    var row = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", id);
    assertThat(row.get("user_id")).isEqualTo(owner.userId());
    assertThat(row.get("source_code")).isEqualTo("print('private source')\r\n");
    assertThat(row.get("total_testcase_count")).isNull();
    assertThat(row.get("score_ratio")).isNull();
    var delivered = message();
    assertThat(new String(delivered.getBody(), StandardCharsets.UTF_8))
        .isEqualTo("{\"submissionId\":\"" + id + "\"}");
    assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void fileUploadUsesTheSameAdmissionAndDoesNotPersistFileName() throws Exception {
    var response =
        submit(request().put("sourceInputMethod", "FILE_UPLOAD").put("fileName", "Main.PY"));
    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(
            jdbc.queryForObject(
                "SELECT source_input_method FROM judge.submissions WHERE problem_id=?",
                String.class,
                owner.problemId()))
        .isEqualTo("FILE_UPLOAD");
  }

  @ParameterizedTest
  @ValueSource(strings = {"INSTRUCTOR", "ADMIN"})
  void nonStudentsCannotCreateAnySubmission(String role) throws Exception {
    UUID id = user(role);
    assertThat(
            submit(
                    token(id, role),
                    owner.problemId().toString(),
                    json.writeValueAsString(request()))
                .statusCode())
        .isEqualTo(403);
    assertThat(count()).isZero();
  }

  @Test
  void missingInvalidAndInactiveAuthenticationCannotWrite() throws Exception {
    String body = json.writeValueAsString(request());
    for (String value : new String[] {null, "invalid"}) {
      assertThat(submit(value, owner.problemId().toString(), body).statusCode()).isEqualTo(401);
    }
    String bearer = token(owner.userId(), "STUDENT");
    jdbc.update("UPDATE identity.users SET status='DEACTIVATED' WHERE id=?", owner.userId());
    assertThat(submit(bearer, owner.problemId().toString(), body).statusCode()).isEqualTo(401);
    assertThat(count()).isZero();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"sourceCode", "language", "submissionModel", "sourceInputMethod", "fileName"})
  void wrongJsonTypesAreNotCoerced(String field) throws Exception {
    var body = request().put("sourceInputMethod", "FILE_UPLOAD").put("fileName", "Main.py");
    body.put(field, 12);
    String code =
        switch (field) {
          case "sourceCode" -> "JDG-V01";
          case "language" -> "JDG-V03";
          case "submissionModel" -> "JDG-V04";
          case "sourceInputMethod" -> "JDG-V05";
          default -> "JDG-V06";
        };
    error(submit(body), 400, code);
    assertThat(count()).isZero();
  }

  @Test
  void byteLimitAndWhitespaceAreEnforcedAtThePublicBoundary() throws Exception {
    error(submit(request().put("sourceCode", " \t\r\n")), 400, "JDG-V01");
    error(submit(request().put("sourceCode", "a".repeat(65537))), 400, "JDG-V02");
    error(submit(request().put("sourceCode", "đ".repeat(32769))), 400, "JDG-V02");
    // This path does not use the private-source assertion in the test queue wrapper.
    controlled.mode = "BOUNDARY";
    var response = submit(request().put("sourceCode", "đ".repeat(32768)));
    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void businessErrorsAndQuotaDoNotPublishOrInsert() throws Exception {
    jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='PYTHON'");
    error(submit(request()), 422, "JDG-102");
    jdbc.update("UPDATE judge.language_configs SET enabled=true WHERE language='PYTHON'");
    jdbc.update(
        "UPDATE problem.problems SET function_wrapper_supported=false WHERE id=?",
        owner.problemId());
    error(submit(request().put("submissionModel", "FUNCTION_WRAPPER")), 422, "JDG-103");
    error(
        submit(request().put("sourceInputMethod", "FILE_UPLOAD").put("fileName", "Main.java")),
        400,
        "JDG-V06");
    jdbc.update(
        "UPDATE problem.problems SET max_submissions_per_hour=1 WHERE id=?", owner.problemId());
    assertThat(submit(request()).statusCode()).isEqualTo(201);
    var quota = submit(request());
    error(quota, 429, "JDG-106");
    assertThat(json.readTree(quota.body()).path("details").path("limit").asInt()).isEqualTo(1);
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void malformedMissingAndUnavailableProblemsFailBeforeWriting() throws Exception {
    String bearer = token(owner.userId(), "STUDENT");
    error(submit(bearer, "bad-id", json.writeValueAsString(request())), 404, "JDG-101");
    error(
        submit(bearer, UUID.randomUUID().toString(), json.writeValueAsString(request())),
        404,
        "JDG-101");
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", owner.problemId());
    error(submit(request()), 404, "JDG-101");
    assertThat(count()).isZero();
  }

  @Test
  void malformedBodiesHaveFixedErrorsWithoutEchoingInput() throws Exception {
    for (String body : new String[] {"{private source", "null", "[]", "{}"}) {
      error(
          submit(token(owner.userId(), "STUDENT"), owner.problemId().toString(), body),
          400,
          "JDG-V01");
    }
    assertThat(count()).isZero();
  }

  @Test
  void databaseFailureUses505WithoutPretendingQueueFailureOrExposingSql() throws Exception {
    jdbc.execute(
        "CREATE FUNCTION judge.reject_http_insert() RETURNS trigger LANGUAGE plpgsql AS $$"
            + " BEGIN RAISE EXCEPTION 'private source'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_http_insert BEFORE INSERT ON judge.submissions"
            + " FOR EACH ROW EXECUTE FUNCTION judge.reject_http_insert()");
    try {
      error(submit(request()), 503, "JDG-505");
      assertThat(count()).isZero();
      assertThat(message()).isNull();
    } finally {
      jdbc.execute("DROP TRIGGER reject_http_insert ON judge.submissions");
      jdbc.execute("DROP FUNCTION judge.reject_http_insert()");
    }
  }

  @Test
  void missingSnapshotAndNoApprovedTestcasesAreRejectedBeforeAdmission() throws Exception {
    jdbc.update("DELETE FROM problem.testcase_set_snapshots WHERE problem_id=?", owner.problemId());
    error(submit(request()), 503, "JDG-504");
    jdbc.update(
        "UPDATE problem.testcases SET approved=false WHERE problem_id=?", owner.problemId());
    error(submit(request()), 422, "JDG-104");
    assertThat(count()).isZero();
    assertThat(message()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"language", "submissionModel", "sourceInputMethod"})
  void unsupportedEnumsAreMappedToTheirExactValidationCodes(String field) throws Exception {
    String code =
        switch (field) {
          case "language" -> "JDG-V03";
          case "submissionModel" -> "JDG-V04";
          default -> "JDG-V05";
        };
    error(submit(request().put(field, "invalid")), 400, code);
    assertThat(count()).isZero();
  }

  @Test
  void lostConfirmAfterWorkerPickupReturnsTicketAndDoesNotOverwriteWorker() throws Exception {
    controlled.mode = "WORKER";
    var response = submit(request());
    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(json.readTree(response.body()).path("status").asString()).isEqualTo("COMPILING");
    UUID id = UUID.fromString(json.readTree(response.body()).path("submissionId").asString());
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM judge.submissions WHERE id=?", String.class, id))
        .isEqualTo("COMPILING");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=?", Long.class, id))
        .isZero();
  }

  @Test
  void lostConfirmFinalizationCommitsOutboxAndPushesOnlyToOwnerBefore503() throws Exception {
    controlled.mode = "LOST";
    String bearer = token(owner.userId(), "STUDENT");
    try (var wire = StompWireClient.open(port);
        var other = StompWireClient.open(port)) {
      wire.connect(bearer);
      assertThat(wire.next()).startsWith("CONNECTED");
      other.connect(token(user("STUDENT"), "STUDENT"));
      assertThat(other.next()).startsWith("CONNECTED");
      wire.subscribed("/user/queue/submissions");
      other.subscribed("/user/queue/submissions");
      var response = submit(request());
      error(response, 503, "JDG-503");
      UUID id =
          UUID.fromString(
              json.readTree(response.body()).path("details").path("submissionId").asString());
      var row =
          jdbc.queryForMap(
              "SELECT status,failure_reason,finished_at FROM judge.submissions WHERE id=?", id);
      assertThat(row.get("status")).isEqualTo("SYSTEM_ERROR");
      assertThat(row.get("failure_reason")).isEqualTo("QUEUE_UNAVAILABLE");
      assertThat(row.get("finished_at")).isNotNull();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=?", Long.class, id))
          .isEqualTo(1);
      String frame = wire.next();
      var event = json.readTree(frame.substring(frame.indexOf("\n\n") + 2));
      assertThat(event.path("submissionId").asString()).isEqualTo(id.toString());
      assertThat(event.path("status").asString()).isEqualTo("SYSTEM_ERROR");
      assertThat(event.path("seq").asInt()).isEqualTo(3);
      assertThat(event.path("isFinal").asBoolean()).isTrue();
      for (String field : List.of("passedCount", "totalCount", "scoreRatio")) {
        assertThat(event.path(field).isNull()).isTrue();
      }
      assertThat(frame).doesNotContain("sourceCode", "input", "expectedOutput", "failureReason");
      other.noMessage();
    }
  }
}
