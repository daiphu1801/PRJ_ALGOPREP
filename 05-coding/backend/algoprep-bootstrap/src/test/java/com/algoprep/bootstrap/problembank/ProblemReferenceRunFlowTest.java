package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
 * #47 over HTTP (DD F2 section 48) with PostgreSQL, Redis and MinIO: the real quota script and the
 * real testcase store; only the sandbox is replaced by a scripted verdict.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemReferenceRunFlowTest {
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
          .withEnv("MINIO_ROOT_USER", "test-testcase-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-testcase-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
    properties.add(
        "algoprep.problem-bank.storage.public-endpoint",
        () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-testcase-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-testcase-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired StringRedisTemplate redis;

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
    script(c -> new ReferenceSolutionRun.CaseResult(c.testcaseId(), true, "AC", 5L, 1000L));
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  private void script(Function<ReferenceSolutionRun.Case, ReferenceSolutionRun.CaseResult> one) {
    org.mockito.Mockito.doAnswer(
            call ->
                new ReferenceSolutionRun.Result(
                    false,
                    ((ReferenceSolutionRun.Request) call.getArgument(0))
                        .cases().stream().map(one).toList()))
        .when(runner)
        .run(any());
  }

  @Test
  void everyApprovedCaseRunsInOrderAndTheReportCarriesNoOutputAndNoWrites() throws Exception {
    var id = problem(true, 3, 1);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','d','INLINE','d','AI',false,99)",
        UUID.randomUUID(),
        id);
    int before = jdbc.queryForObject("SELECT count(*) FROM problem.testcases", Integer.class);

    var response = run(id, teacher, "");

    assertThat(response.getStatus()).isEqualTo(200);
    var body = json(response);
    assertThat(body.path("compileError").isNull()).isTrue();
    assertThat(body.path("summary").path("total").asInt()).isEqualTo(3);
    assertThat(body.path("summary").path("passed").asInt()).isEqualTo(3);
    assertThat(body.path("summary").path("allPass").asBoolean()).isTrue();
    assertThat(body.path("results").get(0).path("position").asInt()).isEqualTo(1);
    assertThat(body.path("results").get(0).path("visibility").asString()).isEqualTo("SAMPLE");
    assertThat(body.path("results").get(0).path("verdict").asString()).isEqualTo("PASS");
    assertThat(body.path("results").get(0).path("runtimeMs").asInt()).isEqualTo(5);
    assertThat(body.path("results").get(0).path("memoryKb").asInt()).isEqualTo(1000);
    assertThat(body.path("ranAt").asString()).isNotBlank();
    assertThat(response.getContentAsString()).doesNotContain("actualOutput").doesNotContain("out1");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.testcases", Integer.class))
        .isEqualTo(before);
  }

  @Test
  void aWrongCaseFailsOnlyItsOwnRowAndAnUnapprovedDraftRunsWhenNamed() throws Exception {
    var id = problem(true, 4, 1);
    var third = caseAt(id, 3);
    script(
        c ->
            new ReferenceSolutionRun.CaseResult(
                c.testcaseId(),
                !c.testcaseId().equals(third),
                c.testcaseId().equals(third) ? "WA" : "AC",
                7L,
                900L));

    var body = json(run(id, teacher, ""));

    assertThat(body.path("summary").path("passed").asInt()).isEqualTo(3);
    assertThat(body.path("summary").path("allPass").asBoolean()).isFalse();
    var row = body.path("results").get(2);
    assertThat(row.path("verdict").asString()).isEqualTo("FAIL");
    assertThat(row.path("failure").asString()).isEqualTo("WRONG_ANSWER");
    assertThat(body.path("results").get(0).path("failure").isNull()).isTrue();

    var draft = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','d','INLINE','d','AI',false,99)",
        draft,
        id);
    script(c -> new ReferenceSolutionRun.CaseResult(c.testcaseId(), true, "AC", 1L, 1L));
    var single = json(run(id, teacher, "{\"testcaseId\":\"" + draft + "\"}"));
    assertThat(single.path("results")).hasSize(1);
    assertThat(single.path("results").get(0).path("testcaseId").asString())
        .isEqualTo(draft.toString());
    var foreign = run(id, teacher, "{\"testcaseId\":\"" + UUID.randomUUID() + "\"}");
    assertThat(foreign.getStatus()).isEqualTo(404);
    assertThat(json(foreign).path("errorCode").asString()).isEqualTo("PRB-011");
    assertThat(run(id, teacher, "{\"testcaseId\":\"nope\"}").getStatus()).isEqualTo(404);
  }

  @Test
  void aCompileErrorMarksEveryRow() throws Exception {
    var id = problem(true, 2, 1);
    org.mockito.Mockito.doReturn(
            new ReferenceSolutionRun.Result(true, List.of(), "Main.java:3: error"))
        .when(runner)
        .run(any());

    var body = json(run(id, teacher, ""));

    assertThat(body.path("compileError").asString()).isEqualTo("Main.java:3: error");
    assertThat(body.path("results").get(0).path("failure").asString()).isEqualTo("COMPILE_ERROR");
    assertThat(body.path("results").get(1).path("verdict").asString()).isEqualTo("FAIL");
    assertThat(body.path("summary").path("passed").asInt()).isZero();
  }

  @Test
  void anOverrideReplacesTheStoredCodeAndBadShapesAreRefused() throws Exception {
    var id = problem(true, 1, 1);

    assertThat(
            run(id, teacher, "{\"language\":\"JAVA\",\"sourceCode\":\"class A {}\"}").getStatus())
        .isEqualTo(200);
    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    org.mockito.Mockito.verify(runner).run(request.capture());
    assertThat(request.getValue().language()).isEqualTo("JAVA");
    assertThat(request.getValue().sourceCode()).isEqualTo("class A {}");

    assertThat(run(id, teacher, "{\"language\":\"JAVA\"}").getStatus()).isEqualTo(400);
    assertThat(run(id, teacher, "{\"sourceCode\":\"x\"}").getStatus()).isEqualTo(400);
    assertThat(run(id, teacher, "{\"language\":\"RUBY\",\"sourceCode\":\"x\"}").getStatus())
        .isEqualTo(400);
    var big = "x".repeat(65_537);
    assertThat(
            run(id, teacher, "{\"language\":\"JAVA\",\"sourceCode\":\"" + big + "\"}").getStatus())
        .isEqualTo(400);
  }

  @Test
  void noSampleSolutionIs422AndNoApprovedCaseIsAnEmptyReport() throws Exception {
    var none = problem(false, 2, 1);
    var refused = run(none, teacher, "");
    assertThat(refused.getStatus()).isEqualTo(422);
    assertThat(json(refused).path("errorCode").asString()).isEqualTo("PRB-040");

    var empty = problem(true, 0, 0);
    var body = json(run(empty, teacher, ""));
    assertThat(body.path("summary").path("total").asInt()).isZero();
    assertThat(body.path("summary").path("allPass").asBoolean()).isFalse();
    assertThat(body.path("results")).isEmpty();
  }

  @Test
  void aLargeSideIsReadFromMinioAndHandedToTheSandbox() throws Exception {
    var id = problem(true, 1, 1);
    var key = "problems/" + id + "/testcases/" + UUID.randomUUID() + "/input-big.txt";
    var text = "L".repeat(9000);
    var bytes = text.getBytes(StandardCharsets.UTF_8);
    minio.putObject(
        PutObjectArgs.builder().bucket(BUCKET).object(key).stream(
                new ByteArrayInputStream(bytes), bytes.length, -1)
            .build());
    jdbc.update(
        "UPDATE problem.testcases SET input_storage='MINIO', input_inline=NULL, input_object_key=? WHERE problem_id=?",
        key,
        id);

    assertThat(run(id, teacher, "").getStatus()).isEqualTo(200);

    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    org.mockito.Mockito.verify(runner).run(request.capture());
    assertThat(request.getValue().cases().get(0).input()).isEqualTo(text);
  }

  @Test
  void theEleventhRunInAMinuteIsRefusedWithTheRetryTime() throws Exception {
    var id = problem(true, 1, 1);
    redis.delete("problem:reference-run:rate:" + teacherId);
    for (int i = 0; i < 10; i++) {
      assertThat(run(id, teacher, "").getStatus()).isEqualTo(200);
    }

    var limited = run(id, teacher, "");

    assertThat(limited.getStatus()).isEqualTo(429);
    var body = json(limited);
    assertThat(body.path("errorCode").asString()).isEqualTo("PRB-055");
    assertThat(body.path("details").path("retryAfterSeconds").asLong()).isBetween(1L, 60L);
    org.mockito.Mockito.verify(runner, org.mockito.Mockito.times(10)).run(any());
  }

  @Test
  void aSandboxFailureIs503AndDoesNotSpendTheQuota() throws Exception {
    var id = problem(true, 1, 1);
    redis.delete("problem:reference-run:rate:" + teacherId);
    org.mockito.Mockito.doThrow(new IllegalStateException("down")).when(runner).run(any());

    var failed = run(id, teacher, "");

    assertThat(failed.getStatus()).isEqualTo(503);
    assertThat(json(failed).path("errorCode").asString()).isEqualTo("PRB-050");
    assertThat(redis.opsForZSet().zCard("problem:reference-run:rate:" + teacherId)).isZero();
  }

  @Test
  void aSandboxAnsweringTooFewCasesIs503AndDoesNotSpendTheQuota() throws Exception {
    var id = problem(true, 2, 1);
    redis.delete("problem:reference-run:rate:" + teacherId);
    // Two approved cases go in, one result comes back: the report must not be built from it.
    org.mockito.Mockito.doReturn(
            new ReferenceSolutionRun.Result(
                false,
                java.util.List.of(
                    new ReferenceSolutionRun.CaseResult(UUID.randomUUID(), true, "AC"))))
        .when(runner)
        .run(any());

    var response = run(id, teacher, "");

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-050");
    assertThat(redis.opsForZSet().zCard("problem:reference-run:rate:" + teacherId)).isZero();
  }

  @Test
  void authorshipAndPermissionsDecideWhoMayRun() throws Exception {
    var draft = problem(true, 1, 1);
    var published = problem(true, 1, 1);
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", published);

    assertThat(run(draft, rival, "").getStatus()).isEqualTo(404);
    var foreign = run(published, rival, "");
    assertThat(foreign.getStatus()).isEqualTo(403);
    assertThat(json(foreign).path("errorCode").asString()).isEqualTo("PRB-003");
    var student403 = run(draft, student, "");
    assertThat(student403.getStatus()).isEqualTo(403);
    assertThat(json(student403).path("errorCode").asString()).isEqualTo("PRB-002");
    assertThat(run(UUID.randomUUID(), teacher, "").getStatus()).isEqualTo(404);
    org.mockito.Mockito.verifyNoInteractions(runner);
  }

  // ---- helpers

  private MockHttpServletResponse run(UUID problem, String token, String body) throws Exception {
    MockHttpServletRequestBuilder request =
        post(BASE + "/" + problem + "/reference-solution/run")
            .header("Authorization", "Bearer " + token);
    if (!body.isEmpty()) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Run actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID caseAt(UUID problem, int position) {
    return jdbc.queryForObject(
        "SELECT id FROM problem.testcases WHERE problem_id=? AND position=?",
        UUID.class,
        problem,
        position);
  }

  /** An UNPUBLISHED problem of the teacher with {@code cases} approved cases, the first SAMPLE. */
  private UUID problem(boolean withSolution, int cases, int samples) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Run fixture','statement',?,?::uuid,'UNPUBLISHED',1000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL);
    if (withSolution) {
      jdbc.update(
          "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON','print(1)')",
          UUID.randomUUID(),
          id);
    }
    var seen = new ArrayList<Integer>();
    for (int i = 1; i <= cases; i++) {
      seen.add(i);
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
          UUID.randomUUID(),
          id,
          i <= samples ? "SAMPLE" : "HIDDEN",
          "in" + i,
          "out" + i,
          i);
    }
    return id;
  }
}
