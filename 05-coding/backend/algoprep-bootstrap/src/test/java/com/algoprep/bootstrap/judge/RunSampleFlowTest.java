package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.SampleQuotaPort;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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

/** Real JWT, PostgreSQL, Redis, F2 sample filtering, F3 packaging and sandbox execution. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=12"
    })
@ActiveProfiles("test")
@Testcontainers
class RunSampleFlowTest {
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
          .withEnv("MINIO_ROOT_USER", "test-sample-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-sample-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    p.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    p.add("algoprep.problem-bank.storage.access-key", () -> "test-sample-user");
    p.add("algoprep.problem-bank.storage.secret-key", () -> "test-sample-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired SampleQuotaPort quota;
  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort access;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @MockitoSpyBean JudgeExecutionPort engine;
  @MockitoSpyBean EvaluateProgramUseCase evaluation;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID student;
  private UUID teacher;
  private UUID problem;
  private final List<UUID> samples = new ArrayList<>();

  @BeforeEach
  void fixture() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    student = user("STUDENT");
    teacher = user("INSTRUCTOR");
    problem = UUID.randomUUID();
    samples.clear();
    redis.delete("judge:run-sample:rate:" + student);
    jdbc.update(
        "UPDATE judge.language_configs SET enabled=true,time_limit_multiplier=CASE language WHEN 'JAVA' THEN 2 WHEN 'PYTHON' THEN 3 ELSE 1 END,memory_limit_multiplier=CASE language WHEN 'JAVA' THEN 2 ELSE 1 END");
    jdbc.update(
        "UPDATE judge.sandbox_settings SET network_access_enabled=false,child_process_limit_enabled=true,return_stderr_to_student=true");
    jdbc.update("UPDATE judge.grading_defaults SET compile_timeout_sec=30");
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,'Sample flow',?,'20000000-0000-4000-8000-000000000001',2000,256,64)",
        problem,
        problem.toString(),
        teacher);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version,function_name,return_type,parameters) VALUES(?,?,'EXACT',1,'echo','{\"kind\":\"INT\"}'::jsonb,'[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]'::jsonb)",
        UUID.randomUUID(),
        problem);
    sample(1, "1\n");
    sample(3, "3\n");
    poisoned("HIDDEN", 2, true);
    poisoned("SAMPLE", 4, false);
    jdbc.update(
        "UPDATE problem.problems SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=?",
        problem);
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Sample user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private void sample(int position, String content) {
    UUID id = UUID.randomUUID();
    samples.add(id);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline) VALUES(?,?,'SAMPLE',?,true,'INLINE',?,'INLINE',?)",
        id,
        problem,
        position,
        content,
        content);
  }

  private void poisoned(String visibility, int position, boolean approved) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_object_key,expected_output_storage,expected_output_object_key) VALUES(?,?,?,?,?,'MINIO','never-read-private-input','MINIO','never-read-private-expected')",
        UUID.randomUUID(),
        problem,
        visibility,
        position,
        approved);
  }

  private String token(UUID id, BaseCategory category) {
    return access.issue(id, category, category.name(), UUID.randomUUID()).value();
  }

  private JsonNode run(String language, String model, String source) throws Exception {
    var body =
        json.createObjectNode()
            .put("language", language)
            .put("submissionModel", model)
            .put("sourceCode", source)
            .put("userId", teacher.toString());
    var response =
        mvc.perform(
                post("/api/v1/judge/problems/" + problem + "/run-sample")
                    .header("Authorization", "Bearer " + token(student, BaseCategory.STUDENT))
                    .contentType("application/json")
                    .content(json.writeValueAsString(body)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return json.readTree(response.getContentAsString());
  }

  private int reject(UUID id, String body, String code) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/judge/problems/" + id + "/run-sample")
                    .header("Authorization", "Bearer " + token(student, BaseCategory.STUDENT))
                    .contentType("application/json")
                    .content(body))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()).path("errorCode").asString())
        .isEqualTo(code);
    return response.getStatus();
  }

  private String request() {
    return "{\"language\":\"PYTHON\",\"submissionModel\":\"STANDARD_IO\",\"sourceCode\":\"print(input())\"}";
  }

  private List<Long> counts() {
    return List.of(
            "submissions", "submission_testcase_results", "judge_run_tokens", "outbox_events")
        .stream()
        .map(table -> jdbc.queryForObject("SELECT count(*) FROM judge." + table, Long.class))
        .toList();
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
          "class Main { public static void main(String[] a) { System.out.println(new java.util.Scanner(System.in).nextInt()); } }";
      case "CPP" -> "#include <iostream>\nint main(){ int n;std::cin>>n;std::cout<<n<<std::endl; }";
      default -> "print(input())\n";
    };
  }

  @ParameterizedTest
  @CsvSource({
    "JAVA,STANDARD_IO",
    "CPP,STANDARD_IO",
    "PYTHON,STANDARD_IO",
    "JAVA,FUNCTION_WRAPPER",
    "CPP,FUNCTION_WRAPPER",
    "PYTHON,FUNCTION_WRAPPER"
  })
  void sixLanguageModelCombinationsUseOnlyApprovedSamplesAndNeverPersist(
      String language, String model) throws Exception {
    var before = counts();
    var result = run(language, model, source(language, model));
    assertThat(result.path("outcome").asString()).isEqualTo("COMPLETED");
    assertThat(result.path("passedCount").asInt()).isEqualTo(2);
    assertThat(result.path("totalCount").asInt()).isEqualTo(2);
    assertThat(result.path("results").get(0).path("testcaseId").asString())
        .isEqualTo(samples.getFirst().toString());
    assertThat(result.path("results").get(1).path("order").asInt()).isEqualTo(2);
    assertThat(result.toString())
        .doesNotContain("never-read", "sourceCode", "expectedOutput", "userConsole");
    assertThat(counts()).isEqualTo(before);
    assertThat(redis.opsForZSet().size("judge:run-sample:rate:" + student)).isEqualTo(1);
    assertThat(redis.hasKey("judge:run-sample:rate:" + teacher)).isFalse();
  }

  @Test
  void mismatchStillRunsEverySampleAndActualOutputIsReturned() throws Exception {
    var result = run("PYTHON", "STANDARD_IO", "print(1)");
    assertThat(result.path("passedCount").asInt()).isEqualTo(1);
    assertThat(result.path("results").get(1).path("verdict").asString()).isEqualTo("WA");
    assertThat(result.path("results").get(1).path("actualOutput").asString()).contains("1");
  }

  @Test
  void mappedCompileFailureIsACharged200WithoutResultRows() throws Exception {
    var before = counts();
    var result = run("PYTHON", "STANDARD_IO", "print(1)\nx=1\nif:\n");
    assertThat(result.path("outcome").asString()).isEqualTo("COMPILE_ERROR");
    assertThat(result.path("compileErrorMessage").asString()).contains("3");
    assertThat(result.path("results").size()).isZero();
    assertThat(redis.opsForZSet().size("judge:run-sample:rate:" + student)).isEqualTo(1);
    assertThat(counts()).isEqualTo(before);
  }

  @Test
  void stderrNeedsStandardIoFlagAndNonAcceptedVerdict() throws Exception {
    var failed =
        run("PYTHON", "STANDARD_IO", "import sys\nprint(0)\nsys.stderr.write('debug-private')");
    assertThat(failed.path("results").get(0).path("stderrSnippet").asString())
        .contains("debug-private");
    jdbc.update("UPDATE judge.sandbox_settings SET return_stderr_to_student=false");
    assertThat(
            run("PYTHON", "STANDARD_IO", "raise ValueError('private')")
                .path("results")
                .get(0)
                .path("stderrSnippet")
                .isNull())
        .isTrue();
    jdbc.update("UPDATE judge.sandbox_settings SET return_stderr_to_student=true");
    assertThat(
            run("PYTHON", "STANDARD_IO", "import sys\nprint(input())\nsys.stderr.write('private')")
                .path("results")
                .get(0)
                .path("stderrSnippet")
                .isNull())
        .isTrue();
    assertThat(
            run(
                    "PYTHON",
                    "FUNCTION_WRAPPER",
                    "class Solution:\n def echo(self,value):\n  raise ValueError('private')\n")
                .path("results")
                .get(0)
                .path("stderrSnippet")
                .isNull())
        .isTrue();
  }

  @Test
  void limitsAreLiveAndStillReturnDisabledLanguages() throws Exception {
    jdbc.update(
        "UPDATE problem.problems SET time_limit_ms=1005,memory_limit_mb=65 WHERE id=?", problem);
    jdbc.update(
        "UPDATE judge.language_configs SET time_limit_multiplier=0.1,memory_limit_multiplier=0.1,enabled=false WHERE language='JAVA'");
    var response =
        mvc.perform(
                get("/api/v1/judge/problems/" + problem + "/execution-limits?language=java")
                    .header("Authorization", "Bearer " + token(student, BaseCategory.STUDENT)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    var row = json.readTree(response.getContentAsString()).path("limits").get(0);
    assertThat(row.path("timeLimitMs").asInt()).isEqualTo(101);
    assertThat(row.path("memoryLimitMb").asInt()).isEqualTo(7);
    assertThat(row.path("enabled").asBoolean()).isFalse();
  }

  @Test
  void validationUnavailableProblemAndRoleGuardsHaveExpectedCodes() throws Exception {
    assertThat(reject(problem, "{}", "JDG-V01")).isEqualTo(400);
    assertThat(reject(problem, "{", "JDG-V01")).isEqualTo(400);
    assertThat(reject(UUID.randomUUID(), request(), "JDG-101")).isEqualTo(404);
    jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='PYTHON'");
    assertThat(reject(problem, request(), "JDG-102")).isEqualTo(422);
    jdbc.update("UPDATE judge.language_configs SET enabled=true WHERE language='PYTHON'");
    jdbc.update(
        "UPDATE problem.testcases SET approved=false WHERE problem_id=? AND visibility='SAMPLE'",
        problem);
    assertThat(reject(problem, request(), "JDG-105")).isEqualTo(422);
    for (String suffix : List.of("run-sample", "execution-limits")) {
      var request =
          suffix.equals("run-sample")
              ? post("/api/v1/judge/problems/" + problem + "/" + suffix)
                  .contentType("application/json")
                  .content(request())
              : get("/api/v1/judge/problems/" + problem + "/" + suffix);
      assertThat(mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(401);
      assertThat(
              mvc.perform(
                      request.header(
                          "Authorization", "Bearer " + token(teacher, BaseCategory.INSTRUCTOR)))
                  .andReturn()
                  .getResponse()
                  .getStatus())
          .isEqualTo(403);
    }
  }

  @Test
  void quotaUsesAtomicRollingWindowAndRetryAfterWithoutCrossUserLeakage() throws Exception {
    for (int i = 0; i < 60; i++) {
      assertThat(quota.reserve(student, UUID.randomUUID()).retryAfterSeconds()).isZero();
    }
    assertThat(reject(problem, request(), "JDG-107")).isEqualTo(429);
    assertThat(quota.reserve(teacher, UUID.randomUUID()).retryAfterSeconds()).isZero();
    var old =
        redis
            .opsForZSet()
            .rangeWithScores("judge:run-sample:rate:" + student, 0, 0)
            .iterator()
            .next();
    redis
        .opsForZSet()
        .add("judge:run-sample:rate:" + student, old.getValue(), old.getScore() - 3600001);
    assertThat(run("PYTHON", "STANDARD_IO", "print(input())").path("passedCount").asInt())
        .isEqualTo(2);
  }

  @Test
  void redisScriptFaultFailsOpenButEngineFailureRefundsOnlyItsOwnEntry() throws Exception {
    redis.opsForValue().set("judge:run-sample:rate:" + student, "wrong-type");
    assertThat(run("PYTHON", "STANDARD_IO", "print(input())").path("passedCount").asInt())
        .isEqualTo(2);
    redis.delete("judge:run-sample:rate:" + student);
    UUID previous = UUID.randomUUID();
    quota.reserve(student, previous);
    doThrow(new JudgeInfrastructureException()).when(engine).compile(any());
    var before = counts();
    assertThat(reject(problem, request(), "JDG-501")).isEqualTo(503);
    assertThat(redis.opsForZSet().range("judge:run-sample:rate:" + student, 0, -1))
        .containsExactly(previous.toString());
    assertThat(counts()).isEqualTo(before);
  }

  @Test
  void ninthConcurrentRequestIsRejectedAndItsQuotaIsRefunded() throws Exception {
    var entered = new CountDownLatch(8);
    var release = new CountDownLatch(1);
    doAnswer(
            call -> {
              entered.countDown();
              if (!release.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test barrier timeout");
              }
              return call.callRealMethod();
            })
        .when(evaluation)
        .evaluate(any());
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<java.util.concurrent.Future<JsonNode>>();
      for (int i = 0; i < 8; i++) {
        futures.add(pool.submit(() -> run("PYTHON", "STANDARD_IO", "print(input())")));
      }
      try {
        assertThat(entered.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(reject(problem, request(), "JDG-502")).isEqualTo(429);
        assertThat(redis.opsForZSet().size("judge:run-sample:rate:" + student)).isEqualTo(8);
      } finally {
        release.countDown();
      }
      for (var future : futures) {
        assertThat(future.get(30, TimeUnit.SECONDS).path("totalCount").asInt()).isEqualTo(2);
      }
    }
  }

  @Test
  void interruptedEngineCallRefundsQuotaEvenWhileThreadIsInterrupted() throws Exception {
    doAnswer(
            call -> {
              Thread.currentThread().interrupt();
              throw new JudgeInfrastructureException();
            })
        .when(engine)
        .compile(any());
    assertThat(reject(problem, request(), "JDG-501")).isEqualTo(503);
    assertThat(redis.opsForZSet().size("judge:run-sample:rate:" + student)).isZero();
  }

  @Test
  void concurrentRedisAdmissionsCannotExceedSixty() throws Exception {
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var calls = new ArrayList<java.util.concurrent.Future<Boolean>>();
      for (int i = 0; i < 70; i++) {
        calls.add(pool.submit(() -> quota.reserve(student, UUID.randomUUID()).recorded()));
      }
      int admitted = 0;
      for (var call : calls) {
        if (call.get(5, TimeUnit.SECONDS)) {
          admitted++;
        }
      }
      assertThat(admitted).isEqualTo(60);
      assertThat(redis.opsForZSet().size("judge:run-sample:rate:" + student)).isEqualTo(60);
    }
  }

  @Test
  void getLimitsRejectsInvalidLanguageDraftDeletedAndMalformedProblem() throws Exception {
    var auth = "Bearer " + token(student, BaseCategory.STUDENT);
    var bad =
        mvc.perform(
                get("/api/v1/judge/problems/" + problem + "/execution-limits?language=RUST")
                    .header("Authorization", auth))
            .andReturn()
            .getResponse();
    assertThat(bad.getStatus()).isEqualTo(400);
    assertThat(json.readTree(bad.getContentAsString()).path("errorCode").asString())
        .isEqualTo("JDG-V03");
    for (String update : List.of("status='UNPUBLISHED'", "status='UNPUBLISHED',deleted=true")) {
      jdbc.update("UPDATE problem.problems SET " + update + " WHERE id=?", problem);
      var response =
          mvc.perform(
                  get("/api/v1/judge/problems/" + problem + "/execution-limits")
                      .header("Authorization", auth))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).isEqualTo(404);
      assertThat(reject(problem, request(), "JDG-101")).isEqualTo(404);
    }
    var malformed =
        mvc.perform(
                get("/api/v1/judge/problems/not-uuid/execution-limits")
                    .header("Authorization", auth))
            .andReturn()
            .getResponse();
    assertThat(malformed.getStatus()).isEqualTo(404);
  }

  @Test
  void sandboxTimeLimitKillsInfiniteLoopAndRunsRemainingSamples() throws Exception {
    jdbc.update("UPDATE problem.problems SET time_limit_ms=100 WHERE id=?", problem);
    var result = run("PYTHON", "STANDARD_IO", "while True: pass");
    assertThat(result.path("totalCount").asInt()).isEqualTo(2);
    assertThat(result.path("results").get(0).path("verdict").asString()).isEqualTo("TLE");
    assertThat(result.path("results").get(1).path("verdict").asString()).isEqualTo("TLE");
  }
}
