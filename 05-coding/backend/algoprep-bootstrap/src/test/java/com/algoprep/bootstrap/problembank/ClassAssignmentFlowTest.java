package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.in.GetAssignedProblemMetricsUseCase;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * #49 to #54 over HTTP (DD F2 sections 50 to 55) with PostgreSQL, the real classes of identity and
 * the real outbox; only the judge metrics port is replaced by scripted numbers.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ClassAssignmentFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/instructor";
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
          .withEnv("MINIO_ROOT_USER", "test-class-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-class-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-class-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-class-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @MockitoBean GetAssignedProblemMetricsUseCase judge;

  // The judge handler is one bean for P6 and P7; replacing it for P6 also removes P7, which
  // StudentJudgeBridges needs to start.
  @MockitoBean com.algoprep.judge.application.ports.in.GetMyClassAssignmentStatesUseCase p7;

  private final Map<UUID, ClassProblemMetric> numbers = new HashMap<>();
  private MockMvc mvc;
  private UUID teacherId;
  private String teacher;
  private String rival;
  private String student;
  private UUID classA;
  private UUID classB;
  private UUID rivalClass;
  private String levelEasy;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    for (String table :
        List.of(
            "class_assignments", "outbox_events", "problem_topics", "problem_tags", "testcases")) {
      jdbc.update("DELETE FROM problem." + table);
    }
    jdbc.update("DELETE FROM problem.worked_examples");
    jdbc.update("DELETE FROM problem.problem_stats");
    jdbc.update("DELETE FROM problem.problems");
    numbers.clear();
    reset(judge);
    when(judge.read(any()))
        .thenAnswer(
            call -> {
              var query =
                  (com.algoprep.judge.application.dto.AssignedProblemMetricsQuery)
                      call.getArgument(0);
              var out = new HashMap<UUID, ClassProblemMetric>();
              for (UUID id : query.problemIds()) {
                out.put(id, numbers.getOrDefault(id, new ClassProblemMetric(0, null)));
              }
              return out;
            });
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = user(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    var rivalId = UUID.randomUUID();
    rival = user(rivalId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    student = user(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    classA = clazz(teacherId, "Class A");
    classB = clazz(teacherId, "Class B");
    rivalClass = clazz(rivalId, "Rival class");
    levelEasy =
        jdbc.queryForObject(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order LIMIT 1",
            String.class);
  }

  @Test
  void aProblemGivenToTwoClassesIsOneRowWithBothClassesAndTheNumbers() throws Exception {
    var both = problem("BOTH", "PUBLISHED", false);
    var onlyA = problem("ONLY-A", "PUBLISHED", false);
    var removed = problem("REMOVED", "PUBLISHED", false);
    var withdrawn = problem("WITHDRAWN", "UNPUBLISHED", false);
    give(both, classA, "2026-03-01T00:00:00Z", null);
    give(both, classB, "2026-03-02T00:00:00Z", null);
    give(onlyA, classA, "2026-03-05T00:00:00Z", null);
    give(removed, classA, "2026-03-03T00:00:00Z", "2026-03-04T00:00:00Z");
    give(withdrawn, classA, "2026-03-06T00:00:00Z", null);
    numbers.put(both, new ClassProblemMetric(7, 50.0));

    var json = body(get(BASE + "/class-assignments", teacher));

    assertThat(json.path("items")).hasSize(3);
    assertThat(json.path("items").get(0).path("title").asString()).isEqualTo("Title WITHDRAWN");
    assertThat(json.path("items").get(0).path("withdrawn").asBoolean()).isTrue();
    var bothRow = json.path("items").get(2);
    assertThat(bothRow.path("title").asString()).isEqualTo("Title BOTH");
    assertThat(bothRow.path("assignedClasses")).hasSize(2);
    assertThat(bothRow.path("submissionCount").asLong()).isEqualTo(7);
    assertThat(bothRow.path("acRatePercent").asDouble()).isEqualTo(50.0);
    assertThat(json.path("items").get(1).path("acRatePercent").isNull()).isTrue();
    assertThat(removed).isNotNull();
    assertThat(body(get(BASE + "/class-assignments?q=only", teacher)).path("items")).hasSize(1);
  }

  @Test
  void aClassFilterNarrowsTheListAndAForeignOrMalformedClassIsRefused() throws Exception {
    var a = problem("A", "PUBLISHED", false);
    var b = problem("B", "PUBLISHED", false);
    give(a, classA, "2026-03-01T00:00:00Z", null);
    give(b, classB, "2026-03-02T00:00:00Z", null);

    var onlyB = body(get(BASE + "/class-assignments?classId=" + classB, teacher));
    assertThat(onlyB.path("items")).hasSize(1);
    assertThat(onlyB.path("items").get(0).path("title").asString()).isEqualTo("Title B");

    var foreign = get(BASE + "/class-assignments?classId=" + rivalClass, teacher);
    assertThat(foreign.getStatus()).isEqualTo(403);
    assertThat(body(foreign).path("errorCode").asString()).isEqualTo("PRB-005");
    assertThat(get(BASE + "/class-assignments?classId=nope", teacher).getStatus()).isEqualTo(403);
    assertThat(body(get(BASE + "/class-assignments", rival)).path("items")).isEmpty();
  }

  @Test
  void whenTheJudgeCannotAnswerTheListStaysAndTheNumbersAreNull() throws Exception {
    var a = problem("A", "PUBLISHED", false);
    give(a, classA, "2026-03-01T00:00:00Z", null);
    doThrow(new IllegalStateException("down")).when(judge).read(any());

    var response = get(BASE + "/class-assignments", teacher);

    assertThat(response.getStatus()).isEqualTo(200);
    var item = body(response).path("items").get(0);
    assertThat(item.path("submissionCount").isNull()).isTrue();
    assertThat(item.path("acRatePercent").isNull()).isTrue();
    assertThat(
            body(get(BASE + "/class-assignments/summary", teacher))
                .path("avgAcRatePercent")
                .isNull())
        .isTrue();
    assertThat(body(get(BASE + "/assigned-problems/top", teacher)).path("items")).isEmpty();
  }

  @Test
  void twentyOneProblemsPageAtTwenty() throws Exception {
    for (int i = 1; i <= 21; i++) {
      give(
          problem(String.format("P%02d", i), "PUBLISHED", false),
          classA,
          "2026-04-" + String.format("%02d", i) + "T00:00:00Z",
          null);
    }

    assertThat(body(get(BASE + "/class-assignments", teacher)).path("items")).hasSize(20);
    var second = body(get(BASE + "/class-assignments?page=2", teacher));
    assertThat(second.path("items")).hasSize(1);
    assertThat(second.path("page").path("totalPages").asInt()).isEqualTo(2);
  }

  @Test
  void theSummaryCountsDistinctProblemsClassesAndAveragesTheRatesRounded() throws Exception {
    var one = problem("S1", "PUBLISHED", false);
    var two = problem("S2", "PUBLISHED", false);
    var three = problem("S3", "PUBLISHED", false);
    give(one, classA, "2026-03-01T00:00:00Z", null);
    give(one, classB, "2026-03-01T00:00:00Z", null);
    give(two, classA, "2026-03-01T00:00:00Z", null);
    give(three, classB, "2026-03-01T00:00:00Z", null);
    numbers.put(one, new ClassProblemMetric(5, 50.0));
    numbers.put(two, new ClassProblemMetric(5, 75.0));

    var json = body(get(BASE + "/class-assignments/summary", teacher));

    assertThat(json.path("assignedProblemCount").asInt()).isEqualTo(3);
    assertThat(json.path("classCount").asInt()).isEqualTo(2);
    assertThat(json.path("avgAcRatePercent").asInt()).isEqualTo(63);
    var none = body(get(BASE + "/class-assignments/summary", rival));
    assertThat(none.path("assignedProblemCount").asInt()).isZero();
    assertThat(none.path("avgAcRatePercent").isNull()).isTrue();
    var empty = user(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    assertThat(body(get(BASE + "/class-assignments/summary", empty)).path("classCount").asInt())
        .isZero();
  }

  @Test
  void theTopFiveAreOrderedBySubmissionCount() throws Exception {
    var ids = new ArrayList<UUID>();
    for (int i = 1; i <= 7; i++) {
      var id = problem("T" + i, "PUBLISHED", false);
      give(id, classA, "2026-03-01T00:00:00Z", null);
      numbers.put(id, new ClassProblemMetric(i * 10L, null));
      ids.add(id);
    }

    var items = body(get(BASE + "/assigned-problems/top", teacher)).path("items");

    assertThat(items).hasSize(5);
    assertThat(items.get(0).path("title").asString()).isEqualTo("Title T7");
    assertThat(items.get(4).path("submissionCount").asLong()).isEqualTo(30);
    assertThat(body(get(BASE + "/assigned-problems/top", rival)).path("items")).isEmpty();
  }

  @Test
  void assignableProblemsExcludeAssignedAndWithdrawnOnesAndListARemovedOneAgain() throws Exception {
    var free = problem("FREE", "PUBLISHED", false);
    var taken = problem("TAKEN", "PUBLISHED", false);
    var back = problem("BACK", "PUBLISHED", false);
    problem("DRAFT", "UNPUBLISHED", false);
    give(taken, classA, "2026-03-01T00:00:00Z", null);
    give(back, classA, "2026-03-01T00:00:00Z", "2026-03-02T00:00:00Z");

    var json = body(get(BASE + "/classes/" + classA + "/assignable-problems", teacher));

    var codes = new ArrayList<String>();
    json.path("items").forEach(i -> codes.add(i.path("code").asString()));
    assertThat(codes).containsExactly("BACK", "FREE");
    assertThat(free).isNotNull();
    assertThat(get(BASE + "/classes/" + rivalClass + "/assignable-problems", teacher).getStatus())
        .isEqualTo(403);
  }

  @Test
  void assigningCreatesRowsAndEventsAndRepeatsAndBadProblemsAreReportedPerProblem()
      throws Exception {
    var a = problem("A", "PUBLISHED", false);
    var b = problem("B", "PUBLISHED", false);
    var draft = problem("DRAFT", "UNPUBLISHED", false);
    var unknown = UUID.randomUUID();

    var first =
        send(
            post(BASE + "/classes/" + classA + "/assignments"), teacher, ids(a, b, draft, unknown));
    var json = body(first);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(json.path("succeededIds")).hasSize(2);
    var failed = new HashMap<String, String>();
    json.path("failedItems")
        .forEach(f -> failed.put(f.path("id").asString(), f.path("code").asString()));
    assertThat(failed)
        .containsEntry(draft.toString(), "PRB-037")
        .containsEntry(unknown.toString(), "PRB-010");
    assertThat(
            count(
                "problem.class_assignments WHERE class_id = '"
                    + classA
                    + "' AND removed_at IS NULL"))
        .isEqualTo(2);
    assertThat(count("problem.outbox_events WHERE event_type = 'ProblemAssignedToClass'"))
        .isEqualTo(2);

    var again = body(send(post(BASE + "/classes/" + classA + "/assignments"), teacher, ids(a, b)));
    assertThat(again.path("succeededIds")).isEmpty();
    assertThat(again.path("skippedIds")).hasSize(2);
    assertThat(count("problem.class_assignments WHERE class_id = '" + classA + "'")).isEqualTo(2);
    assertThat(count("problem.outbox_events WHERE event_type = 'ProblemAssignedToClass'"))
        .isEqualTo(2);
  }

  @Test
  void assignRefusesBadSizesForeignClassesAndMissingPermission() throws Exception {
    var a = problem("A", "PUBLISHED", false);
    var path = BASE + "/classes/" + classA + "/assignments";

    assertThat(send(post(path), teacher, "{\"problemIds\":[]}").getStatus()).isEqualTo(400);
    var fiftyOne = new ArrayList<UUID>();
    for (int i = 0; i < 51; i++) {
      fiftyOne.add(UUID.randomUUID());
    }
    assertThat(send(post(path), teacher, ids(fiftyOne.toArray(new UUID[0]))).getStatus())
        .isEqualTo(400);
    var foreign = send(post(BASE + "/classes/" + rivalClass + "/assignments"), teacher, ids(a));
    assertThat(foreign.getStatus()).isEqualTo(403);
    assertThat(body(foreign).path("errorCode").asString()).isEqualTo("PRB-005");
    var denied = send(post(path), student, ids(a));
    assertThat(denied.getStatus()).isEqualTo(403);
    assertThat(body(denied).path("errorCode").asString()).isEqualTo("PRB-002");
    assertThat(count("problem.class_assignments")).isZero();
  }

  @Test
  void anEventThatCannotBeWrittenRollsBackOnlyThatProblem() throws Exception {
    var a = problem("A", "PUBLISHED", false);
    jdbc.execute(
        "ALTER TABLE problem.outbox_events ADD CONSTRAINT test_block_assign CHECK (event_type <> 'ProblemAssignedToClass') NOT VALID");
    try {
      var response = send(post(BASE + "/classes/" + classA + "/assignments"), teacher, ids(a));

      assertThat(response.getStatus()).isEqualTo(200);
      var json = body(response);
      assertThat(json.path("succeededIds")).isEmpty();
      assertThat(json.path("failedItems").get(0).path("code").asString()).isEqualTo("PRB-056");
      assertThat(count("problem.class_assignments")).isZero();
    } finally {
      jdbc.execute("ALTER TABLE problem.outbox_events DROP CONSTRAINT test_block_assign");
    }
    var retry = body(send(post(BASE + "/classes/" + classA + "/assignments"), teacher, ids(a)));
    assertThat(retry.path("succeededIds")).hasSize(1);
    assertThat(count("problem.class_assignments")).isEqualTo(1);
  }

  @Test
  void removingIsSoftReportsAConflictTheSecondTimeAndAllowsAssigningAgain() throws Exception {
    var a = problem("A", "PUBLISHED", false);
    var draft = problem("DRAFT", "UNPUBLISHED", false);
    give(a, classA, "2026-03-01T00:00:00Z", null);
    give(draft, classA, "2026-03-01T00:00:00Z", null);
    var path = BASE + "/classes/" + classA + "/assignments/" + a;

    assertThat(send(delete(path), teacher, "").getStatus()).isEqualTo(204);
    assertThat(
            jdbc.queryForObject(
                "SELECT removed_at IS NOT NULL FROM problem.class_assignments WHERE problem_id = ?",
                Boolean.class,
                a))
        .isTrue();
    assertThat(count("problem.outbox_events WHERE event_type = 'ProblemUnassignedFromClass'"))
        .isEqualTo(1);
    var twice = send(delete(path), teacher, "");
    assertThat(twice.getStatus()).isEqualTo(409);
    assertThat(body(twice).path("errorCode").asString()).isEqualTo("PRB-025");
    assertThat(
            send(delete(BASE + "/classes/" + classA + "/assignments/" + draft), teacher, "")
                .getStatus())
        .isEqualTo(204);
    assertThat(
            send(delete(BASE + "/classes/" + rivalClass + "/assignments/" + a), teacher, "")
                .getStatus())
        .isEqualTo(403);

    var reassigned =
        body(send(post(BASE + "/classes/" + classA + "/assignments"), teacher, ids(a)));
    assertThat(reassigned.path("succeededIds")).hasSize(1);
    assertThat(count("problem.class_assignments WHERE problem_id = '" + a + "'")).isEqualTo(2);
    assertThat(
            count(
                "problem.class_assignments WHERE problem_id = '" + a + "' AND removed_at IS NULL"))
        .isEqualTo(1);
  }

  @Test
  void aCallWithoutATokenIs401() throws Exception {
    for (String path :
        List.of("/class-assignments", "/class-assignments/summary", "/assigned-problems/top")) {
      assertThat(
              mvc.perform(
                      org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                          BASE + path))
                  .andReturn()
                  .getResponse()
                  .getStatus())
          .as(path)
          .isEqualTo(401);
    }
  }

  // ---- helpers

  private MockHttpServletResponse get(String path, String token) throws Exception {
    return mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                .header("Authorization", "Bearer " + token))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String token, String json) throws Exception {
    request.header("Authorization", "Bearer " + token);
    if (!json.isEmpty()) {
      request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode body(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static String ids(UUID... problems) {
    return "{\"problemIds\":["
        + java.util.Arrays.stream(problems)
            .map(id -> "\"" + id + "\"")
            .collect(java.util.stream.Collectors.joining(","))
        + "]}";
  }

  private String user(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Class actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID clazz(UUID instructor, String name) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES (?,?,?)", id, instructor, name);
    return id;
  }

  private UUID problem(String code, String status, boolean deleted) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'statement',?,?::uuid,?,?,1000,256,64)",
        id,
        code,
        "Title " + code,
        UUID.randomUUID(),
        levelEasy,
        status,
        deleted);
    return id;
  }

  private void give(UUID problem, UUID clazz, String at, String removedAt) {
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at,removed_at) VALUES (?,?,?,?,?::timestamptz,?::timestamptz)",
        UUID.randomUUID(),
        problem,
        clazz,
        teacherId,
        at,
        removedAt);
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
  }
}
