package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Management reads #24, #25 and #26 over HTTP with real PostgreSQL and JWT permissions. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemAuthoringListFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/authoring/problems";

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
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  private MockMvc mvc;
  private UUID teacherId;
  private UUID rivalId;
  private String teacher;
  private String admin;
  private String student;
  private List<UUID> levels;
  private List<UUID> topics;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // The tests share one database, so each starts from an empty bank.
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.problem_stats");
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM problem.testcases");
    jdbc.update("DELETE FROM problem.worked_examples");
    jdbc.update("DELETE FROM problem.problem_specs");
    jdbc.update("DELETE FROM problem.problem_reference_solutions");
    jdbc.update("DELETE FROM problem.problem_tags");
    jdbc.update("DELETE FROM problem.problems");
    levels =
        jdbc.queryForList("SELECT id FROM problem.problem_levels ORDER BY sort_order", UUID.class);
    topics = jdbc.queryForList("SELECT id FROM problem.topics ORDER BY sort_order", UUID.class);
    teacherId = UUID.randomUUID();
    rivalId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    token(rivalId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  @Test
  void anAdminSeesTheWholeBankWithUnfilteredTotalsAndAnInstructorOnlyOwnProblems()
      throws Exception {
    problem(teacherId, "Two Sum", "PUBLISHED", "0 days");
    problem(teacherId, "Three Sum", "UNPUBLISHED", "0 days");
    problem(rivalId, "Rival Problem", "PUBLISHED", "0 days");

    var all = json(call(admin, ""));
    assertThat(all.path("items")).hasSize(3);
    assertThat(all.path("totals").path("total").asInt()).isEqualTo(3);
    assertThat(all.path("totals").path("published").asInt()).isEqualTo(2);

    var own = json(call(teacher, "?q=Rival"));
    assertThat(own.path("items")).isEmpty(); // a matching search never widens the scope
    assertThat(own.path("totals").path("total").asInt()).isEqualTo(2);
    assertThat(json(call(teacher, "")).path("items")).hasSize(2);
  }

  @Test
  void aSoftDeletedProblemNeverShowsUnderAnyFilter() throws Exception {
    var gone = problem(teacherId, "Gone", "UNPUBLISHED", "0 days");
    jdbc.update("UPDATE problem.problems SET deleted = true WHERE id = ?", gone);

    for (String query : new String[] {"", "?status=UNPUBLISHED", "?q=Gone"}) {
      assertThat(json(call(admin, query)).path("items")).as(query).isEmpty();
    }
    assertThat(json(call(admin, "")).path("totals").path("total").asInt()).isZero();
  }

  @Test
  void sortingByLevelFollowsTheSortOrderNotTheName() throws Exception {
    var first = problem(teacherId, "Aaa", "UNPUBLISHED", "0 days");
    var last = problem(teacherId, "Zzz", "UNPUBLISHED", "0 days");
    jdbc.update(
        "UPDATE problem.problems SET level_id = ? WHERE id = ?",
        levels.get(levels.size() - 1),
        first);
    jdbc.update("UPDATE problem.problems SET level_id = ? WHERE id = ?", levels.get(0), last);

    var ids = ids(json(call(admin, "?sort=level&dir=asc")));

    assertThat(ids).containsExactly(last.toString(), first.toString());
  }

  @Test
  void rowsCarryTopicsCountsAndNullStatisticsWhenThereAreNone() throws Exception {
    var id = problem(teacherId, "Counted", "PUBLISHED", "0 days");
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
        id,
        topics.get(0));
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
        id,
        topics.get(1));
    testcase(id, 1);
    testcase(id, 2);
    jdbc.update(
        "INSERT INTO problem.class_assignments(id, problem_id, class_id, assigned_by) VALUES (?,?,?,?)",
        UUID.randomUUID(),
        id,
        UUID.randomUUID(),
        teacherId);
    jdbc.update(
        "INSERT INTO problem.class_assignments(id, problem_id, class_id, assigned_by, removed_at) VALUES (?,?,?,?, now())",
        UUID.randomUUID(),
        id,
        UUID.randomUUID(),
        teacherId);

    var item = json(call(admin, "")).path("items").get(0);

    assertThat(item.path("topicNames")).hasSize(2);
    assertThat(item.path("testcaseCount").asInt()).isEqualTo(2);
    assertThat(item.path("assignedClassCount").asInt())
        .isEqualTo(1); // a removed assignment is not counted
    assertThat(item.path("submissionCount").asInt()).isZero();
    assertThat(item.path("acRate").isNull()).isTrue();
    assertThat(item.path("levelCode").asString()).isNotBlank();
  }

  @Test
  void paginationClampsAPagePastTheEndAndMetadataIsExact() throws Exception {
    for (int i = 0; i < 9; i++) {
      problem(teacherId, "P" + i, "UNPUBLISHED", "0 days");
    }

    var last = json(call(admin, "?page=99"));

    assertThat(last.path("page").path("number").asInt()).isEqualTo(2);
    assertThat(last.path("page").path("totalPages").asInt()).isEqualTo(2);
    assertThat(last.path("page").path("totalItems").asInt()).isEqualTo(9);
    assertThat(last.path("items")).hasSize(1);
  }

  @Test
  void searchMetacharactersAreLiteralAndBoundariesAreStrict() throws Exception {
    problem(teacherId, "100% sure", "UNPUBLISHED", "0 days");
    problem(teacherId, "Other", "UNPUBLISHED", "0 days");

    assertThat(json(call(admin, "?q=%25")).path("items")).hasSize(1);
    assertThat(call(admin, "?q=" + "x".repeat(100)).getStatus()).isEqualTo(200);
    for (String bad :
        new String[] {
          "?q=" + "x".repeat(101),
          "?pageSize=7",
          "?sort=nope",
          "?dir=up",
          "?status=DRAFT",
          "?pageSize=abc"
        }) {
      var response = call(admin, bad);
      assertThat(response.getStatus()).as(bad).isEqualTo(400);
      assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-001");
    }
  }

  @Test
  void aStudentIs403AndNoSessionIs401() throws Exception {
    assertThat(call(student, "").getStatus()).isEqualTo(403);
    assertThat(call(student, "/attention").getStatus()).isEqualTo(403);
    assertThat(call(student, "/topic-distribution").getStatus()).isEqualTo(403);
    assertThat(mvc.perform(get(BASE)).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void topicDistributionCountsOnlyProblemsInScopeWithAnEmptyBankSafe() throws Exception {
    var empty = json(call(admin, "/topic-distribution"));
    assertThat(empty.path("totalProblemsWithTopic").asInt()).isZero();
    assertThat(empty.path("items")).isEmpty();

    for (int i = 0; i < 4; i++) {
      var id = problem(teacherId, "T" + i, "UNPUBLISHED", "0 days");
      if (i < 2) {
        jdbc.update(
            "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
            id,
            topics.get(0));
      }
    }
    var rival = problem(rivalId, "Rival", "UNPUBLISHED", "0 days");
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
        rival,
        topics.get(1));

    var mine = json(call(teacher, "/topic-distribution"));
    assertThat(mine.path("totalProblemsWithTopic").asInt()).isEqualTo(2);
    assertThat(mine.path("items")).hasSize(1);
    assertThat(mine.path("items").get(0).path("count").asInt()).isEqualTo(2);
    assertThat(mine.path("items").get(0).path("percent").asDouble()).isEqualTo(100.0);

    var all = json(call(admin, "/topic-distribution"));
    assertThat(all.path("totalProblemsWithTopic").asInt()).isEqualTo(3);
    assertThat(all.path("totalTopicsUsed").asInt()).isEqualTo(2);
    assertThat(all.path("items").get(0).path("percent").asDouble()).isEqualTo(66.7);
  }

  @Test
  void attentionCountsTheFourRulesWithTheirBoundariesAndSkipsDeleted() throws Exception {
    problem(teacherId, "No testcase", "UNPUBLISHED", "0 days"); // NO_TESTCASE
    var low = problem(teacherId, "Low AC", "PUBLISHED", "0 days");
    var edge = problem(teacherId, "Edge AC", "PUBLISHED", "0 days");
    var unrated = problem(teacherId, "Unrated", "PUBLISHED", "0 days");
    testcase(low, 1);
    testcase(edge, 1);
    testcase(unrated, 1);
    jdbc.update(
        "INSERT INTO problem.problem_stats(problem_id, submission_count, accepted_count, ac_rate) VALUES (?, 10, 2, 20.00)",
        low);
    jdbc.update(
        "INSERT INTO problem.problem_stats(problem_id, submission_count, accepted_count, ac_rate) VALUES (?, 10, 3, 30.00)",
        edge);
    var hidden = problem(teacherId, "Hidden", "UNPUBLISHED", "0 days");
    testcase(hidden, 1);
    jdbc.update(
        "UPDATE problem.problems SET published_at = now() WHERE id = ?",
        hidden); // HIDDEN_FROM_LEARNERS
    var stale = problem(teacherId, "Stale", "UNPUBLISHED", "8 days");
    var fresh = problem(teacherId, "Fresh", "UNPUBLISHED", "6 days 23 hours");
    testcase(stale, 1);
    testcase(fresh, 1);
    var deleted = problem(teacherId, "Deleted", "UNPUBLISHED", "30 days");
    jdbc.update("UPDATE problem.problems SET deleted = true WHERE id = ?", deleted);

    var items = json(call(teacher, "/attention")).path("items");

    assertThat(items).hasSize(4);
    assertThat(codes(items))
        .containsExactly("NO_TESTCASE", "LOW_AC", "HIDDEN_FROM_LEARNERS", "STALE_DRAFT");
    assertThat(items.get(0).path("count").asInt()).isEqualTo(1); // the deleted one is not counted
    assertThat(items.get(1).path("count").asInt()).isEqualTo(1); // 20 yes, 30 no, unrated no
    assertThat(items.get(2).path("count").asInt()).isEqualTo(1);
    assertThat(items.get(3).path("count").asInt()).isEqualTo(1); // 8 days yes, 6d23h no
  }

  // ---- helpers

  private MockHttpServletResponse call(String token, String suffix) throws Exception {
    return mvc.perform(
            get(java.net.URI.create(BASE + suffix)).header("Authorization", "Bearer " + token))
        .andReturn()
        .getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static List<String> ids(JsonNode body) {
    var out = new ArrayList<String>();
    body.path("items").forEach(i -> out.add(i.path("id").asString()));
    return out;
  }

  private static List<String> codes(JsonNode items) {
    var out = new ArrayList<String>();
    items.forEach(i -> out.add(i.path("ruleCode").asString()));
    return out;
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'List actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** A problem of {@code author} whose last edit was {@code age} ago. */
  private UUID problem(UUID author, String title, String status, String age) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,updated_at) VALUES (?,?,?,'s',?,?,?,1000,256,64, now() - CAST(? AS interval))",
        id,
        id.toString(),
        title,
        author,
        levels.get(0),
        status,
        age);
    return id;
  }

  private void testcase(UUID problem, int position) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','i','INLINE','o','MANUAL',true,?)",
        UUID.randomUUID(),
        problem,
        position);
  }
}
