package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
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

/**
 * #4 and #13A over HTTP (DD F2 sections 5 and 5A) with PostgreSQL, the real class membership of
 * identity and the real per-assignment state of the judge module (F4 P7); only the failure cases
 * replace one bridge answer.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class MyClassAssignmentsFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/me";
  private static final String BUCKET = "algoprep-testcases";
  private static final Instant NOW = Instant.now();

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
          .withEnv("MINIO_ROOT_USER", "test-mine-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-mine-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-mine-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-mine-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @MockitoSpyBean StudentJudgeBridges bridges;

  private MockMvc mvc;
  private UUID me;
  private UUID teacher;
  private String student;
  private String admin;
  private String levelEasy;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM problem.outbox_events");
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.problems");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    me = UUID.randomUUID();
    student = user(me, "STUDENT", BaseCategory.STUDENT);
    teacher = UUID.randomUUID();
    user(teacher, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = user(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    levelEasy =
        jdbc.queryForObject(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order LIMIT 1",
            String.class);
  }

  @Test
  void groupsShowThePublishedProblemsOfCurrentClassesWithTheStateOfEachAssignment()
      throws Exception {
    var classA = clazz("Class A");
    var left = clazz("Left class");
    enroll(classA, me, null);
    enroll(left, me, NOW.minusSeconds(3600));
    var solved = problem("SOLVED", "PUBLISHED", false);
    var tried = problem("TRIED", "PUBLISHED", false);
    var early = problem("EARLY", "PUBLISHED", false);
    var hidden = problem("HIDDEN", "UNPUBLISHED", false);
    var removed = problem("REMOVED", "PUBLISHED", false);
    var inLeft = problem("INLEFT", "PUBLISHED", false);
    give(solved, classA, ago(5), null);
    give(tried, classA, ago(4), null);
    give(early, classA, ago(3), null);
    give(hidden, classA, ago(2), null);
    give(removed, classA, ago(6), ago(1));
    give(inLeft, left, ago(2), null);
    submission(me, solved, ago(4), "ACCEPTED");
    submission(me, tried, ago(3), "WRONG_ANSWER");
    submission(me, early, ago(4), "ACCEPTED");

    var json = body(call(BASE + "/class-assignments", student));

    assertThat(json.path("groups")).hasSize(1);
    var group = json.path("groups").get(0);
    assertThat(group.path("classId").asString()).isEqualTo(classA.toString());
    assertThat(group.path("className").asString()).isEqualTo("Class A");
    assertThat(group.path("items"))
        .extracting(i -> i.path("title").asString())
        .containsExactly("Title EARLY", "Title TRIED", "Title SOLVED");
    var states = new ArrayList<String>();
    group.path("items").forEach(i -> states.add(i.path("solveState").asString()));
    assertThat(states).containsExactly("UNSOLVED", "ATTEMPTED", "SOLVED");
  }

  @Test
  void aReassignedProblemIsJudgedOnItsCurrentAssignmentOnly() throws Exception {
    var classA = clazz("Class A");
    enroll(classA, me, null);
    var again = problem("AGAIN", "PUBLISHED", false);
    give(again, classA, ago(10), ago(6));
    give(again, classA, ago(3), null);
    submission(me, again, ago(8), "ACCEPTED");

    var items =
        body(call(BASE + "/class-assignments", student)).path("groups").get(0).path("items");

    assertThat(items).hasSize(1);
    assertThat(items.get(0).path("solveState").asString()).isEqualTo("UNSOLVED");
  }

  @Test
  void onlyTheThreeNewestGroupsAreReturned() throws Exception {
    for (int i = 0; i < 4; i++) {
      var clazz = clazz("Class " + i);
      enroll(clazz, me, null);
      give(problem("P" + i, "PUBLISHED", false), clazz, ago(10 - i), null);
    }

    var groups = body(call(BASE + "/class-assignments", student)).path("groups");

    assertThat(groups)
        .extracting(g -> g.path("className").asString())
        .containsExactly("Class 3", "Class 2", "Class 1");
  }

  @Test
  void noClassMeansNoGroupsAndAnUnreachableIdentityHidesTheBlock() throws Exception {
    assertThat(call(BASE + "/class-assignments", student).getStatus()).isEqualTo(200);
    assertThat(body(call(BASE + "/class-assignments", student)).path("groups")).isEmpty();

    var classA = clazz("Class A");
    enroll(classA, me, null);
    give(problem("P", "PUBLISHED", false), classA, ago(2), null);
    doReturn(Optional.empty()).when(bridges).activeClassesOf(any());

    assertThat(body(call(BASE + "/class-assignments", student)).path("groups")).isEmpty();
  }

  @Test
  void aFailingJudgeLeavesTheStateNullButKeepsTheRows() throws Exception {
    var classA = clazz("Class A");
    enroll(classA, me, null);
    give(problem("P", "PUBLISHED", false), classA, ago(2), null);
    doReturn(Optional.empty()).when(bridges).read(any(java.util.Collection.class));

    var response = call(BASE + "/class-assignments", student);

    assertThat(response.getStatus()).isEqualTo(200);
    var item = body(response).path("groups").get(0).path("items").get(0);
    assertThat(item.path("title").asString()).isEqualTo("Title P");
    assertThat(item.path("solveState").isNull()).isTrue();
  }

  @Test
  void classProblemsKeepWithdrawnProblemsDropRemovedOnesAndOrderByClassThenNewest()
      throws Exception {
    var classA = clazz("Class A");
    var classB = clazz("Class B");
    enroll(classA, me, null);
    enroll(classB, me, null);
    var live = problem("LIVE", "PUBLISHED", false);
    var draft = problem("DRAFT", "UNPUBLISHED", false);
    var gone = problem("GONE", "PUBLISHED", true);
    var removed = problem("REMOVED", "PUBLISHED", false);
    var other = problem("OTHER", "PUBLISHED", false);
    give(live, classA, ago(5), null);
    give(draft, classA, ago(4), null);
    give(gone, classA, ago(3), null);
    give(removed, classA, ago(6), ago(1));
    give(other, classB, ago(2), null);
    submission(me, live, ago(2), "ACCEPTED");

    var page = body(call(BASE + "/class-problems", student));
    var all = page.path("items");

    assertThat(page.path("page").path("number").asInt()).isEqualTo(1);
    assertThat(page.path("page").path("size").asInt()).isEqualTo(20);
    assertThat(page.path("page").path("totalItems").asLong()).isEqualTo(4);
    assertThat(page.path("page").path("totalPages").asInt()).isEqualTo(1);
    assertThat(all).hasSize(4);
    var first = classA.toString().compareTo(classB.toString()) < 0 ? classA : classB;
    assertThat(all.get(0).path("classId").asString()).isEqualTo(first.toString());
    var inA = new ArrayList<JsonNode>();
    all.forEach(
        i -> {
          if (i.path("classId").asString().equals(classA.toString())) {
            inA.add(i);
          }
        });
    assertThat(inA)
        .extracting(i -> i.path("code").asString())
        .containsExactly("GONE", "DRAFT", "LIVE");
    assertThat(inA)
        .extracting(i -> i.path("withdrawn").asBoolean())
        .containsExactly(true, true, false);
    assertThat(inA.get(2).path("solveState").asString()).isEqualTo("SOLVED");
    assertThat(inA.get(0).path("solveState").asString()).isEqualTo("UNSOLVED");
    assertThat(inA.get(2).path("levelDisplayName").asString()).isNotBlank();

    var onlyB = body(call(BASE + "/class-problems?classId=" + classB, student)).path("items");
    assertThat(onlyB).hasSize(1);
    assertThat(onlyB.get(0).path("code").asString()).isEqualTo("OTHER");
  }

  @Test
  void classProblemsArePagedByPageAndPageSizeWithLenientBadValues() throws Exception {
    var classA = clazz("Class A");
    enroll(classA, me, null);
    for (int i = 0; i < 25; i++) {
      var p = problem(String.format("P%02d", i), "PUBLISHED", false);
      give(p, classA, ago(100 - i), null);
      if (i % 5 == 0) {
        submission(me, p, ago(100 - i - 1), "ACCEPTED");
      }
    }

    var first = body(call(BASE + "/class-problems", student));
    assertThat(first.path("items")).hasSize(20);
    assertThat(first.path("page").path("totalItems").asLong()).isEqualTo(25);
    assertThat(first.path("page").path("totalPages").asInt()).isEqualTo(2);
    assertThat(first.path("items").get(0).path("code").asString()).isEqualTo("P24");
    assertThat(first.path("meta").path("totalAssigned").asInt()).isEqualTo(25);
    assertThat(first.path("meta").path("solvedCount").asInt()).isEqualTo(5);

    var second = body(call(BASE + "/class-problems?page=2", student));
    assertThat(second.path("items")).hasSize(5);
    assertThat(second.path("items").get(4).path("code").asString()).isEqualTo("P00");
    assertThat(second.path("items").get(0).path("solveState").asString()).isEqualTo("UNSOLVED");
    assertThat(second.path("meta")).isEqualTo(first.path("meta"));

    var beyond = body(call(BASE + "/class-problems?page=99&pageSize=10", student));
    assertThat(beyond.path("page").path("number").asInt()).isEqualTo(3);
    assertThat(beyond.path("items")).hasSize(5);

    var lenient = body(call(BASE + "/class-problems?page=abc&pageSize=500", student));
    assertThat(lenient.path("page").path("number").asInt()).isEqualTo(1);
    assertThat(lenient.path("page").path("size").asInt()).isEqualTo(20);
    assertThat(lenient.path("items")).hasSize(20);
  }

  @Test
  void classProblemsMetaCountsTheWholeClassAndANullCountWhenTheWholeSetReadFails()
      throws Exception {
    var classA = clazz("Class A");
    enroll(classA, me, null);
    for (int i = 0; i < 25; i++) {
      var p = problem(String.format("M%02d", i), "PUBLISHED", false);
      give(p, classA, ago(100 - i), null);
      if (i % 5 == 0) {
        submission(me, p, ago(100 - i - 1), "ACCEPTED");
      }
    }

    var first = body(call(BASE + "/class-problems", student));
    var second = body(call(BASE + "/class-problems?page=2", student));
    assertThat(first.path("meta").path("totalAssigned").asInt()).isEqualTo(25);
    assertThat(first.path("meta").path("solvedCount").asInt()).isEqualTo(5);
    assertThat(second.path("meta")).isEqualTo(first.path("meta"));

    // The page read (20 keys) answers, the whole-class read (25 keys) fails.
    doReturn(Optional.empty())
        .when(bridges)
        .read(
            org.mockito.ArgumentMatchers
                .<java.util.Collection<
                        com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey>>
                    argThat(k -> k.size() == 25));
    var degraded = body(call(BASE + "/class-problems", student));
    assertThat(degraded.path("items")).hasSize(20);
    assertThat(degraded.path("meta").path("totalAssigned").asInt()).isEqualTo(25);
    assertThat(degraded.path("meta").path("solvedCount").isNull()).isTrue();
  }

  @Test
  void aClassThatIsMalformedForeignUnknownOrLeftIsTheSame404() throws Exception {
    var mine = clazz("Mine");
    var foreign = clazz("Foreign");
    var left = clazz("Left");
    enroll(mine, me, null);
    enroll(left, me, NOW.minusSeconds(60));

    for (var raw :
        List.of("not-a-uuid", UUID.randomUUID().toString(), foreign.toString(), left.toString())) {
      var response = call(BASE + "/class-problems?classId=" + raw, student);
      assertThat(response.getStatus()).as(raw).isEqualTo(404);
      assertThat(body(response).path("errorCode").asString()).isEqualTo("PRB-015");
    }
  }

  @Test
  void classProblemsAre503NotAGuessWhenIdentityOrTheJudgeCannotAnswer() throws Exception {
    var classA = clazz("Class A");
    enroll(classA, me, null);
    give(problem("P", "PUBLISHED", false), classA, ago(2), null);

    doReturn(Optional.empty()).when(bridges).read(any(java.util.Collection.class));
    var state = call(BASE + "/class-problems", student);
    assertThat(state.getStatus()).isEqualTo(503);
    assertThat(body(state).path("errorCode").asString()).isEqualTo("PRB-057");

    doReturn(Optional.empty()).when(bridges).activeClassesOf(any());
    var classes = call(BASE + "/class-problems", student);
    assertThat(classes.getStatus()).isEqualTo(503);
    assertThat(body(classes).path("errorCode").asString()).isEqualTo("PRB-058");
  }

  @Test
  void onlyAStudentReadsClassProblemsAndEveryoneMustBeSignedIn() throws Exception {
    assertThat(call(BASE + "/class-problems", admin).getStatus()).isEqualTo(403);
    assertThat(call(BASE + "/class-problems", null).getStatus()).isEqualTo(401);
    assertThat(call(BASE + "/class-assignments", null).getStatus()).isEqualTo(401);
  }

  private MockHttpServletResponse call(String path, String token) throws Exception {
    var request = get(path);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode body(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static Instant ago(int days) {
    return NOW.minusSeconds(days * 86_400L);
  }

  private String user(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Class actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID clazz(String name) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES (?,?,?)", id, teacher, name);
    return id;
  }

  private void enroll(UUID classId, UUID who, Instant left) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at,left_at) VALUES (?,?,?,CAST(? AS timestamptz),CAST(? AS timestamptz))",
        UUID.randomUUID(),
        classId,
        who,
        NOW.minusSeconds(30L * 86_400).toString(),
        left == null ? null : left.toString());
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
        deleted ? "UNPUBLISHED" : status,
        deleted);
    return id;
  }

  private void give(UUID problem, UUID clazz, Instant at, Instant removedAt) {
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at,removed_at) VALUES (?,?,?,?,CAST(? AS timestamptz),CAST(? AS timestamptz))",
        UUID.randomUUID(),
        problem,
        clazz,
        teacher,
        at.toString(),
        removedAt == null ? null : removedAt.toString());
  }

  private void submission(UUID owner, UUID problem, Instant at, String status) {
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','class(){}','EDITOR',?,?,1,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        UUID.randomUUID(),
        owner,
        problem,
        status,
        "ACCEPTED".equals(status) ? java.math.BigDecimal.ONE : null,
        at.toString(),
        at.toString());
  }
}
