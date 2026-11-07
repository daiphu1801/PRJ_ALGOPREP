package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * #39 over HTTP (DD F2 section 40) with PostgreSQL and MinIO for the real snapshot kernel. Only the
 * sandbox is replaced: the reference-solution run itself needs go-judge and is covered by the
 * bridge, so here the port answers with a scripted verdict and records what it was asked.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemPublishStatusHttpFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/authoring/problems";
  private static final String LEVEL = "20000000-0000-4000-8000-000000000001";
  private static final String BUCKET = "algoprep-testcases";

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
          .withEnv("MINIO_ROOT_USER", "test-publish-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-publish-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-publish-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-publish-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @MockitoBean SampleSolutionRunnerPort runner;

  private MockMvc mvc;
  private UUID teacherId;
  private String teacher;
  private String rival;
  private String student;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    reset(runner);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  @Test
  void aCompleteProblemPublishesAfterTheSolutionPassesEveryApprovedCase() throws Exception {
    var id = problem("UNPUBLISHED", 8, 2, 2, true);
    allPass();

    var response = status(id, "PUBLISHED", teacher);

    assertThat(response.getStatus()).isEqualTo(200);
    var json = json(response);
    assertThat(json.path("status").asString()).isEqualTo("PUBLISHED");
    assertThat(json.path("publishedAt").asString()).isNotBlank();
    assertThat(json.path("checklist").path("sampleSolutionAllPass").asBoolean()).isTrue();
    assertThat(column(id, "status")).isEqualTo("PUBLISHED");
    assertThat(count("problem.testcase_set_snapshots WHERE problem_id = '" + id + "'"))
        .isEqualTo(1);
    assertThat(
            count(
                "problem.outbox_events WHERE aggregate_id = '"
                    + id
                    + "' AND event_type = 'ProblemPublished'"))
        .isEqualTo(1);
    assertThat(
            count(
                "identity.system_audit_logs WHERE action_type = 'PROBLEM_PUBLISHED' AND target_id = '"
                    + id
                    + "'"))
        .isEqualTo(1);
    var sent = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(runner).run(sent.capture());
    assertThat(sent.getValue().cases()).hasSize(8);
    assertThat(sent.getValue().language()).isEqualTo("PYTHON");
  }

  @Test
  void aShortChecklistIs422WithEveryMissingConditionAndWritesNothing() throws Exception {
    var id = problem("UNPUBLISHED", 7, 1, 1, false);
    allPass();

    var response = status(id, "PUBLISHED", teacher);

    assertThat(response.getStatus()).isEqualTo(422);
    var json = json(response);
    assertThat(json.path("errorCode").asString()).isEqualTo("PRB-030");
    var codes = new java.util.ArrayList<String>();
    json.path("details").path("items").forEach(i -> codes.add(i.path("code").asString()));
    assertThat(codes)
        .containsExactly(
            "MIN_APPROVED_TESTCASES",
            "MIN_APPROVED_SAMPLES",
            "MIN_WORKED_EXAMPLES",
            "REFERENCE_SOLUTION_PASSES_ALL",
            "SPEC_DECLARED");
    // No reference solution, so the sandbox has nothing to run.
    verifyNoInteractions(runner);
    assertThat(column(id, "status")).isEqualTo("UNPUBLISHED");
    assertThat(count("problem.testcase_set_snapshots WHERE problem_id = '" + id + "'")).isZero();
    assertThat(count("problem.outbox_events WHERE aggregate_id = '" + id + "'")).isZero();
  }

  @Test
  void aFailingSolutionIs422AndAnUnavailableSandboxIs503() throws Exception {
    var id = problem("UNPUBLISHED", 8, 2, 2, true);
    when(runner.run(any()))
        .thenAnswer(
            call ->
                new ReferenceSolutionRun.Result(
                    false,
                    ((ReferenceSolutionRun.Request) call.getArgument(0))
                        .cases().stream()
                            .map(
                                c ->
                                    new ReferenceSolutionRun.CaseResult(
                                        c.testcaseId(), false, "WA"))
                            .toList()));

    var failing = status(id, "PUBLISHED", teacher);
    assertThat(failing.getStatus()).isEqualTo(422);
    assertThat(json(failing).path("details").path("items").get(0).path("code").asString())
        .isEqualTo("REFERENCE_SOLUTION_PASSES_ALL");

    reset(runner);
    when(runner.run(any())).thenThrow(new IllegalStateException("sandbox down"));
    var down = status(id, "PUBLISHED", teacher);
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(json(down).path("errorCode").asString()).isEqualTo("PRB-050");
    assertThat(column(id, "status")).isEqualTo("UNPUBLISHED");
  }

  @Test
  void withdrawingNeedsNoChecklistAndRepublishingIsIdempotent() throws Exception {
    var id = problem("PUBLISHED", 0, 0, 0, false);

    var down = status(id, "UNPUBLISHED", teacher);
    assertThat(down.getStatus()).isEqualTo(200);
    assertThat(json(down).path("status").asString()).isEqualTo("UNPUBLISHED");
    assertThat(column(id, "status")).isEqualTo("UNPUBLISHED");
    assertThat(
            count(
                "identity.system_audit_logs WHERE action_type = 'PROBLEM_UNPUBLISHED' AND target_id = '"
                    + id
                    + "'"))
        .isEqualTo(1);
    verifyNoInteractions(runner);

    var again = status(id, "UNPUBLISHED", teacher);
    assertThat(again.getStatus()).isEqualTo(200);
    assertThat(
            count(
                "identity.system_audit_logs WHERE action_type = 'PROBLEM_UNPUBLISHED' AND target_id = '"
                    + id
                    + "'"))
        .isEqualTo(1);
  }

  @Test
  void permissionsAuthorshipAndTheOptimisticLockAreEnforced() throws Exception {
    var draft = problem("UNPUBLISHED", 8, 2, 2, true);
    var published = problem("PUBLISHED", 0, 0, 0, false);

    assertThat(status(draft, "PUBLISHED", student).getStatus()).isEqualTo(403);
    assertThat(status(draft, "PUBLISHED", rival).getStatus()).isEqualTo(404);
    var forbidden = status(published, "UNPUBLISHED", rival);
    assertThat(forbidden.getStatus()).isEqualTo(403);
    assertThat(json(forbidden).path("errorCode").asString()).isEqualTo("PRB-003");
    assertThat(column(published, "status")).isEqualTo("PUBLISHED");

    var stale = put(BASE + "/" + draft + "/status");
    var response =
        send(
            stale,
            teacher,
            "{\"status\":\"PUBLISHED\",\"expectedUpdatedAt\":\"2020-01-01T00:00:00Z\"}");
    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-020");

    var bad =
        send(
            put(BASE + "/" + draft + "/status"),
            teacher,
            "{\"status\":\"ARCHIVED\",\"expectedUpdatedAt\":\"" + stamp(draft) + "\"}");
    assertThat(bad.getStatus()).isEqualTo(400);
    assertThat(json(bad).path("errorCode").asString()).isEqualTo("PRB-001");
    verifyNoInteractions(runner);
  }

  // ---- helpers

  private void allPass() {
    when(runner.run(any()))
        .thenAnswer(
            call ->
                new ReferenceSolutionRun.Result(
                    false,
                    ((ReferenceSolutionRun.Request) call.getArgument(0))
                        .cases().stream()
                            .map(
                                c ->
                                    new ReferenceSolutionRun.CaseResult(c.testcaseId(), true, "AC"))
                            .toList()));
  }

  private MockHttpServletResponse status(UUID id, String target, String token) throws Exception {
    return send(
        put(BASE + "/" + id + "/status"),
        token,
        "{\"status\":\"" + target + "\",\"expectedUpdatedAt\":\"" + stamp(id) + "\"}");
  }

  private MockHttpServletResponse send(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String token,
      String body)
      throws Exception {
    request
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Publish actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** A problem with {@code approved} approved cases ({@code samples} of them SAMPLE). */
  private UUID problem(String status, int approved, int samples, int examples, boolean complete) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Publish fixture','statement',?,?::uuid,?,1000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL,
        status);
    for (int i = 1; i <= examples; i++) {
      jdbc.update(
          "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          i,
          "i" + i,
          "o" + i);
    }
    for (int i = 1; i <= approved; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
          UUID.randomUUID(),
          id,
          i <= samples ? "SAMPLE" : "HIDDEN",
          "in" + i,
          "out" + i,
          i);
    }
    if (complete) {
      jdbc.update(
          "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON','def solve(): pass')",
          UUID.randomUUID(),
          id);
      jdbc.update(
          "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'in','out','solve','{\"kind\":\"INT\"}'::jsonb,'[]'::jsonb)",
          UUID.randomUUID(),
          id);
    }
    return id;
  }

  private String column(UUID id, String name) {
    return jdbc.queryForObject(
        "SELECT " + name + " FROM problem.problems WHERE id=?", String.class, id);
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
  }

  private String stamp(UUID id) {
    return jdbc.queryForObject(
            "SELECT updated_at FROM problem.problems WHERE id=?", Timestamp.class, id)
        .toInstant()
        .toString();
  }
}
