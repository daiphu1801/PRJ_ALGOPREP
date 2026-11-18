package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.ArrayList;
import java.util.List;
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
 * #1, #2, #5, #6, #7 over HTTP (DD F2 sections 2, 3, 6, 7, 8) with PostgreSQL and MinIO and the
 * real solve-state bridge into identity.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class PublishedProblemReadFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/problems";
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
          .withEnv("MINIO_ROOT_USER", "test-read-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-read-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-read-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-read-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private MockMvc mvc;
  private UUID me;
  private String token;
  private String levelEasy;
  private String levelHard;
  private String tag;

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
    jdbc.update("DELETE FROM problem.problems");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    me = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Reader',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        me,
        me + "@test.local");
    token = tokens.issue(me, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
    var levels =
        jdbc.queryForList(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order", String.class);
    levelEasy = levels.get(0);
    levelHard = levels.get(levels.size() - 1);
    tag = UUID.randomUUID().toString().substring(0, 8);
    jdbc.update("INSERT INTO problem.tags(id,name) VALUES (?,?)", UUID.randomUUID(), tag);
  }

  @Test
  void theListHoldsOnlyPublishedProblemsAndPagesWithTheirSolveState() throws Exception {
    var ids = new ArrayList<UUID>();
    for (int i = 1; i <= 15; i++) {
      ids.add(problem(String.format("P%02d", i), "PUBLISHED", false, levelEasy));
    }
    var draft = problem("DRAFT", "UNPUBLISHED", false, levelEasy);
    problem("GONE", "PUBLISHED", true, levelEasy);
    best(ids.get(0), "ACCEPTED");
    best(ids.get(1), "WRONG_ANSWER");

    var first = body(call(BASE, "page", "1"));
    assertThat(first.path("items")).hasSize(12);
    assertThat(first.path("page").path("totalItems").asLong()).isEqualTo(15);
    assertThat(first.path("page").path("totalPages").asInt()).isEqualTo(2);
    assertThat(first.path("solveStateAvailable").asBoolean()).isTrue();
    assertThat(first.path("items").get(0).path("code").asString()).isEqualTo("P01");
    assertThat(first.path("items").get(0).path("solveState").asString()).isEqualTo("SOLVED");
    assertThat(first.path("items").get(1).path("solveState").asString()).isEqualTo("ATTEMPTED");
    assertThat(first.path("items").get(2).path("solveState").asString()).isEqualTo("UNSOLVED");
    assertThat(first.path("items").get(0).path("acRate").isNull()).isTrue();
    var past = body(call(BASE, "page", "99"));
    assertThat(past.path("page").path("number").asInt()).isEqualTo(2);
    assertThat(past.path("items")).hasSize(3);
    var typed = body(call(BASE, "q", "draft"));
    assertThat(typed.path("items")).isEmpty();
    assertThat(draft).isNotNull();
  }

  @Test
  void filtersAddUpAndTheStatusFilterFollowsTheCallersOwnState() throws Exception {
    var topic = topic("Graphs");
    var a = problem("A-ONE", "PUBLISHED", false, levelEasy);
    var b = problem("B-TWO", "PUBLISHED", false, levelHard);
    var c = problem("C-THREE", "PUBLISHED", false, levelHard);
    link(b, topic);
    link(c, topic);
    tagged(c);
    best(b, "ACCEPTED");
    best(c, "TIME_LIMIT_EXCEEDED");

    assertThat(codes(call(BASE, "difficulty", code(levelHard))))
        .containsExactly("B-TWO", "C-THREE");
    assertThat(codes(call(BASE, "topic", topic.toString()))).containsExactly("B-TWO", "C-THREE");
    assertThat(codes(call(BASE, "tag", tag.toUpperCase()))).containsExactly("C-THREE");
    assertThat(codes(call(BASE, "status", "SOLVED"))).containsExactly("B-TWO");
    assertThat(codes(call(BASE, "status", "ATTEMPTED"))).containsExactly("C-THREE");
    assertThat(codes(call(BASE, "status", "UNSOLVED"))).containsExactly("A-ONE");
    assertThat(codes(call(BASE, "status", "UNSOLVED", "difficulty", code(levelHard)))).isEmpty();
    assertThat(codes(call(BASE, "q", "b-t"))).containsExactly("B-TWO");
    assertThat(a).isNotNull();
  }

  @Test
  void anInvalidParameterIsIgnoredAndNeverAnError() throws Exception {
    problem("ONLY", "PUBLISHED", false, levelEasy);

    var response =
        call(
            BASE,
            "difficulty",
            "no-such-level",
            "sort",
            "abc",
            "dir",
            "sideways",
            "page",
            "-4",
            "pageSize",
            "abc",
            "status",
            "WHATEVER",
            "topic",
            "not-a-uuid");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(body(response).path("items")).hasSize(1);
    assertThat(body(response).path("page").path("size").asInt()).isEqualTo(12);
    assertThat(call(BASE, "q", "x".repeat(101)).getStatus()).isEqualTo(200);
    problem("100%-SURE", "PUBLISHED", false, levelEasy);
    assertThat(codes(call(BASE, "q", "100%"))).containsExactly("100%-SURE");
  }

  @Test
  void acRateSortsMissingValuesLastInBothDirectionsAndIsNullWithoutStats() throws Exception {
    var low = problem("LOW", "PUBLISHED", false, levelEasy);
    var high = problem("HIGH", "PUBLISHED", false, levelEasy);
    problem("NONE", "PUBLISHED", false, levelEasy);
    stats(low, 10, 2);
    stats(high, 10, 9);

    assertThat(codes(call(BASE, "sort", "acRate", "dir", "asc")))
        .containsExactly("LOW", "HIGH", "NONE");
    assertThat(codes(call(BASE, "sort", "acRate", "dir", "desc")))
        .containsExactly("HIGH", "LOW", "NONE");
    var items = body(call(BASE, "sort", "acRate", "dir", "desc")).path("items");
    assertThat(items.get(0).path("acRate").decimalValue().doubleValue()).isEqualTo(90.0);
    assertThat(items.get(2).path("acRate").isNull()).isTrue();
    assertThat(codes(call(BASE, "sort", "level", "dir", "desc"))).hasSize(3);
  }

  @Test
  void randomPicksAPublishedProblemAndHonoursTheSolveFilter() throws Exception {
    var solved = problem("SOLVED-ONE", "PUBLISHED", false, levelEasy);
    var open = problem("OPEN-ONE", "PUBLISHED", false, levelEasy);
    problem("DRAFT-ONE", "UNPUBLISHED", false, levelEasy);
    best(solved, "ACCEPTED");

    for (int i = 0; i < 10; i++) {
      var picked =
          body(call(BASE + "/random", "status", "NOT_SOLVED")).path("problemId").asString();
      assertThat(picked).isEqualTo(open.toString());
    }
    var any = body(call(BASE + "/random")).path("problemId").asString();
    assertThat(List.of(solved.toString(), open.toString())).contains(any);
    var none = body(call(BASE + "/random", "q", "nothing-matches"));
    assertThat(none.path("problemId").isNull()).isTrue();
  }

  @Test
  void theWorkspaceCarriesTheStatementAndNothingFromTheAuthorSide() throws Exception {
    var id = problem("WORK", "PUBLISHED", false, levelEasy);
    var topic = topic("Math");
    link(id, topic);
    example(id, 2, "in2", "out2", null);
    example(id, 1, "in1", "out1", "because");
    jdbc.update("INSERT INTO problem.bookmarks(user_id,problem_id) VALUES (?,?)", me, id);
    spec(id);
    stats(id, 4, 1);

    var response = call(BASE + "/" + id + "/workspace");

    assertThat(response.getStatus()).isEqualTo(200);
    var json = body(response);
    assertThat(json.path("bookmarked").asBoolean()).isTrue();
    assertThat(json.path("topicNames").get(0).asString()).startsWith("Math");
    assertThat(json.path("stdinFormatMd").asString()).isEqualTo("stdin format");
    assertThat(json.path("workedExamples").get(0).path("input").asString()).isEqualTo("in1");
    assertThat(json.path("workedExamples").get(0).path("explanationMd").asString())
        .isEqualTo("because");
    assertThat(json.path("workedExamples").get(1).path("explanationMd").isNull()).isTrue();
    assertThat(json.path("acRate").decimalValue().doubleValue()).isEqualTo(25.0);
    for (String forbidden :
        List.of("testcases", "referenceSolution", "aiBrief", "authorId", "status", "limits")) {
      assertThat(json.has(forbidden)).as(forbidden).isFalse();
    }
  }

  @Test
  void anUnpublishedDeletedOrMissingProblemIsTheSame404() throws Exception {
    var draft = problem("D1", "UNPUBLISHED", false, levelEasy);
    var gone = problem("D2", "PUBLISHED", true, levelEasy);
    var unknown = UUID.randomUUID();
    var bodies = new ArrayList<String>();

    for (String suffix : List.of("/workspace", "/overview", "/sample-testcases")) {
      for (String id : List.of(draft.toString(), gone.toString(), unknown.toString(), "nope")) {
        var response = call(BASE + "/" + id + suffix);
        assertThat(response.getStatus()).as(id + suffix).isEqualTo(404);
        var json = body(response);
        assertThat(json.path("errorCode").asString()).isEqualTo("PRB-010");
        bodies.add(json.path("detail").asString());
      }
    }
    assertThat(bodies).containsOnly("Problem not found");
  }

  @Test
  void anEmptyConstraintsAndNoExamplesAreEmptyNotErrors() throws Exception {
    var id = problem("EMPTY", "PUBLISHED", false, levelEasy);

    var json = body(call(BASE + "/" + id + "/workspace"));

    assertThat(json.path("constraintsMd").asString()).isEmpty();
    assertThat(json.path("workedExamples")).isEmpty();
    assertThat(json.path("stdinFormatMd").isNull()).isTrue();
  }

  @Test
  void theOverviewCountsHiddenCasesWithoutExposingAnythingAboutThem() throws Exception {
    var id = problem("OVER", "PUBLISHED", false, levelEasy);
    example(id, 1, "i", "o", "why");
    testcase(id, 1, "SAMPLE", true, "s-in", "s-out");
    testcase(id, 2, "HIDDEN", true, "secret-in", "secret-out");
    testcase(id, 3, "HIDDEN", true, "secret-in-2", "secret-out-2");
    testcase(id, 4, "HIDDEN", false, "draft-in", "draft-out");

    var response = call(BASE + "/" + id + "/overview");

    var json = body(response);
    assertThat(json.path("hiddenTestcaseCount").asInt()).isEqualTo(2);
    assertThat(json.path("examples").get(0).path("explanationMd").asString()).isEqualTo("why");
    assertThat(response.getContentAsString()).doesNotContain("secret").doesNotContain("draft-in");
    assertThat(json.has("testcases")).isFalse();
    var bare = problem("BARE", "PUBLISHED", false, levelEasy);
    assertThat(body(call(BASE + "/" + bare + "/overview")).path("hiddenTestcaseCount").asInt())
        .isZero();
  }

  @Test
  void onlyApprovedSamplesAreListedAndALargeOneIsALink() throws Exception {
    var id = problem("SAMP", "PUBLISHED", false, levelEasy);
    spec(id);
    testcase(id, 1, "SAMPLE", true, "a", "b");
    testcase(id, 2, "SAMPLE", false, "unapproved-in", "unapproved-out");
    testcase(id, 3, "HIDDEN", true, "hidden-in", "hidden-out");
    var big = UUID.randomUUID();
    var key = "problems/" + id + "/testcases/" + big + "/input.txt";
    minio.putObject(
        io.minio.PutObjectArgs.builder().bucket(BUCKET).object(key).stream(
                new java.io.ByteArrayInputStream("large".getBytes()), 5, -1)
            .build());
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_object_key,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'SAMPLE','MINIO',?,'INLINE','big-out','MANUAL',true,4)",
        big,
        id,
        key);

    var response = call(BASE + "/" + id + "/sample-testcases");

    var json = body(response);
    assertThat(json.path("parameterNames").get(0).asString()).isEqualTo("nums");
    assertThat(json.path("items")).hasSize(2);
    assertThat(json.path("items").get(0).path("input").asString()).isEqualTo("a");
    assertThat(json.path("items").get(0).path("order").asInt()).isEqualTo(1);
    assertThat(json.path("items").get(0).path("testcaseId").asString()).isNotBlank();
    var large = json.path("items").get(1);
    assertThat(large.path("input").isNull()).isTrue();
    assertThat(large.path("inputDownloadUrl").asString()).contains(key);
    assertThat(response.getContentAsString())
        .doesNotContain("hidden-in")
        .doesNotContain("unapproved");
    var none = problem("NOSAMP", "PUBLISHED", false, levelEasy);
    assertThat(body(call(BASE + "/" + none + "/sample-testcases")).path("items")).isEmpty();
  }

  @Test
  void aCallWithoutATokenIs401() throws Exception {
    assertThat(mvc.perform(get(BASE)).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  // ---- helpers

  private MockHttpServletResponse call(String path, String... queryPairs) throws Exception {
    var request = get(path).header("Authorization", "Bearer " + token);
    for (int i = 0; i < queryPairs.length; i += 2) {
      request.param(queryPairs[i], queryPairs[i + 1]);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode body(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static List<String> codes(MockHttpServletResponse response) throws Exception {
    var codes = new ArrayList<String>();
    body(response).path("items").forEach(i -> codes.add(i.path("code").asString()));
    return codes;
  }

  private String code(String levelId) {
    return jdbc.queryForObject(
        "SELECT code FROM problem.problem_levels WHERE id = ?::uuid", String.class, levelId);
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

  private UUID topic(String name) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.topics(id,code,name,sort_order) VALUES (?,?,?,1)",
        id,
        name.toLowerCase() + "-" + id,
        name + "-" + id.toString().substring(0, 4));
    return id;
  }

  private void link(UUID problem, UUID topic) {
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES (?,?)", problem, topic);
  }

  private void tagged(UUID problem) {
    jdbc.update(
        "INSERT INTO problem.problem_tags(problem_id,tag_id) SELECT ?,id FROM problem.tags WHERE name=?",
        problem,
        tag);
  }

  private void best(UUID problem, String verdict) {
    jdbc.update(
        "INSERT INTO identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict) VALUES (?,?,?,?)",
        me,
        problem,
        "ACCEPTED".equals(verdict) ? 1 : 0.5,
        verdict);
  }

  private void stats(UUID problem, int submissions, int accepted) {
    jdbc.update(
        "INSERT INTO problem.problem_stats(problem_id,submission_count,accepted_count,ac_rate) VALUES (?,?,?,?)",
        problem,
        submissions,
        accepted,
        accepted * 100.0 / submissions);
  }

  private void example(UUID problem, int position, String in, String out, String why) {
    jdbc.update(
        "INSERT INTO problem.worked_examples(id,problem_id,position,input,output,explanation_md) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        problem,
        position,
        in,
        out,
        why);
  }

  private void spec(UUID problem) {
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'stdin format','stdout format','solve','{\"kind\":\"INT\"}'::jsonb,'[{\"name\":\"nums\",\"type\":{\"kind\":\"INT\"}}]'::jsonb)",
        UUID.randomUUID(),
        problem);
  }

  private void testcase(
      UUID problem, int position, String visibility, boolean approved, String... inAndOut) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',?,?)",
        UUID.randomUUID(),
        problem,
        visibility,
        inAndOut[0],
        inAndOut[1],
        approved,
        position);
  }
}
