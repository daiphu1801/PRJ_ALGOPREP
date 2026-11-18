package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
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
 * #12 over HTTP (DD F2 section 13) with PostgreSQL, the real solve state of identity and the real
 * per-topic counts of the judge module (F4 P5), including the problems that were withdrawn; only
 * the failure case replaces the judge answer.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class SuggestedProblemsFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String URL = "/api/v1/problem-bank/suggested-problems";
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
          .withEnv("MINIO_ROOT_USER", "test-suggest-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-suggest-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-suggest-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-suggest-password");
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
  private String student;
  private String admin;
  private String levelEasy;
  private String levelHard;
  private UUID weak;
  private UUID strong;
  private UUID untouched;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    jdbc.update("DELETE FROM problem.problem_tags");
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.bookmarks");
    jdbc.update("DELETE FROM problem.testcases");
    jdbc.update("DELETE FROM problem.worked_examples");
    jdbc.update("DELETE FROM problem.problem_specs");
    jdbc.update("DELETE FROM problem.problem_stats");
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM problem.outbox_events");
    jdbc.update("DELETE FROM problem.problems");
    jdbc.update("DELETE FROM problem.topics");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    me = UUID.randomUUID();
    student = user(me, "STUDENT", BaseCategory.STUDENT);
    admin = user(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    var levels =
        jdbc.queryForList(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order", String.class);
    levelEasy = levels.get(0);
    levelHard = levels.get(levels.size() - 1);
    weak = topic("Weak", 1);
    strong = topic("Strong", 2);
    untouched = topic("Untouched", 3);
  }

  /**
   * Weak topic: seven published problems, one solved, six submissions of which one Accepted, two on
   * withdrawn problems that still count in the history: score 100 * (0.7 / 6 + 0.3 / 7) = 16.
   */
  private List<UUID> scenario() {
    var published = new ArrayList<UUID>();
    for (int i = 1; i <= 7; i++) {
      var p = problem("W" + i, "PUBLISHED", false, levelEasy);
      link(p, weak);
      published.add(p);
    }
    var s1 = problem("S1", "PUBLISHED", false, levelEasy);
    link(s1, strong);
    var e1 = problem("E1", "PUBLISHED", false, levelHard);
    link(e1, untouched);
    var draft = problem("X1", "UNPUBLISHED", false, levelEasy);
    var gone = problem("X2", "UNPUBLISHED", true, levelEasy);
    link(draft, weak);
    link(gone, weak);
    best(published.get(0));
    best(s1);
    submission(published.get(0), "ACCEPTED");
    submission(published.get(1), "WRONG_ANSWER");
    submission(published.get(2), "WRONG_ANSWER");
    submission(published.get(3), "TIME_LIMIT_EXCEEDED");
    submission(draft, "WRONG_ANSWER");
    submission(gone, "WRONG_ANSWER");
    submission(s1, "ACCEPTED");
    return published;
  }

  @Test
  void problemsAreRankedByTheWeakestTopicAndTheVectorIsReturned() throws Exception {
    scenario();

    var json = body(call(student));

    assertThat(json.path("rankingAvailable").asBoolean()).isTrue();
    assertThat(codes(json)).containsExactly("W2", "W3", "W4", "W5", "W6", "W7", "E1");
    assertThat(json.path("items").get(0).path("solveState").asString()).isEqualTo("UNSOLVED");
    assertThat(json.path("items").get(0).path("topicName").asString())
        .isEqualTo("Weak-" + weak.toString().substring(0, 4));
    var scores = json.path("topicScores");
    assertThat(scores)
        .extracting(s -> s.path("topicId").asString())
        .containsExactly(weak.toString(), strong.toString());
    assertThat(scores).extracting(s -> s.path("score").asInt()).containsExactly(16, 100);
  }

  @Test
  void aTopicOrDifficultyFilterNarrowsTheRowsButNotTheVector() throws Exception {
    scenario();

    var byTopic = body(call(student, "topic", strong.toString()));
    assertThat(codes(byTopic)).containsExactly("S1");
    assertThat(byTopic.path("items").get(0).path("solveState").asString()).isEqualTo("SOLVED");
    assertThat(byTopic.path("topicScores")).hasSize(2);

    var hard =
        jdbc.queryForObject(
            "SELECT code FROM problem.problem_levels WHERE id = ?::uuid", String.class, levelHard);
    assertThat(codes(body(call(student, "difficulty", hard)))).containsExactly("E1");
    assertThat(codes(body(call(student, "difficulty", "no-such-level")))).hasSize(7);
  }

  @Test
  void aTopicThatIsNotAUuidIsA400() throws Exception {
    var response = call(student, "topic", "nope");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(body(response).path("errorCode").asString()).isEqualTo("PRB-001");
  }

  @Test
  void aFailingJudgeGateKeepsTheProblemsOrderedBySolveStateThenCode() throws Exception {
    scenario();
    doReturn(Optional.empty()).when(bridges).read(anyMap());

    var json = body(call(student));

    assertThat(json.path("rankingAvailable").asBoolean()).isFalse();
    assertThat(json.path("topicScores")).isEmpty();
    assertThat(codes(json)).containsExactly("E1", "W2", "W3", "W4", "W5", "W6", "W7");
  }

  @Test
  void aCallerThatIsNotAStudentGetsTheRowsWithoutRanking() throws Exception {
    scenario();

    var json = body(call(admin));

    assertThat(json.path("rankingAvailable").asBoolean()).isFalse();
    assertThat(json.path("topicScores")).isEmpty();
    assertThat(json.path("items")).hasSize(7);
  }

  @Test
  void anEmptyCatalogueIsAnEmptyListAndAnonymousIsRefused() throws Exception {
    var json = body(call(student));

    assertThat(json.path("items")).isEmpty();
    assertThat(json.path("rankingAvailable").asBoolean()).isTrue();
    assertThat(call(null).getStatus()).isEqualTo(401);
  }

  private MockHttpServletResponse call(String token, String... queryPairs) throws Exception {
    var request = get(URL);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    for (int i = 0; i < queryPairs.length; i += 2) {
      request.param(queryPairs[i], queryPairs[i + 1]);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode body(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static List<String> codes(JsonNode json) {
    var codes = new ArrayList<String>();
    json.path("items").forEach(i -> codes.add(i.path("code").asString()));
    return codes;
  }

  private String user(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Reader',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID topic(String name, int sortOrder) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.topics(id,code,name,sort_order) VALUES (?,?,?,?)",
        id,
        name.toLowerCase() + "-" + id,
        name + "-" + id.toString().substring(0, 4),
        sortOrder);
    return id;
  }

  private UUID problem(String code, String status, boolean deleted, String level) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'statement',?,?::uuid,?,?,1000,256,64)",
        id,
        code,
        "Title " + code,
        UUID.randomUUID(),
        level,
        deleted ? "UNPUBLISHED" : status,
        deleted);
    return id;
  }

  private void link(UUID problem, UUID topic) {
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES (?,?)", problem, topic);
  }

  private void best(UUID problem) {
    jdbc.update(
        "INSERT INTO identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict) VALUES (?,?,1,'ACCEPTED')",
        me,
        problem);
  }

  private void submission(UUID problem, String status) {
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','class(){}','EDITOR',?,?,1,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        UUID.randomUUID(),
        me,
        problem,
        status,
        "ACCEPTED".equals(status) ? java.math.BigDecimal.ONE : null,
        NOW.minusSeconds(3600).toString(),
        NOW.minusSeconds(3600).toString());
  }
}
