package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
 * #3, #8 to #11, #13 and the level progress over HTTP (DD F2 sections 4, 9 to 12, 14, 14A) with
 * PostgreSQL and the real solve-state bridge into identity.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class StudentCatalogFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank";
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
          .withEnv("MINIO_ROOT_USER", "test-catalog-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-catalog-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-catalog-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-catalog-password");
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
  void topicProgressCountsOnlyVisibleProblemsAndKeepsEmptyTopics() throws Exception {
    var arrays = topic("Arrays");
    var empty = topic("Empty");
    var a = problem("A", "PUBLISHED", false, levelEasy);
    var b = problem("B", "PUBLISHED", false, levelEasy);
    var draft = problem("C", "UNPUBLISHED", false, levelEasy);
    var gone = problem("D", "UNPUBLISHED", true, levelEasy);
    for (var p : List.of(a, b, draft, gone)) {
      link(p, arrays);
    }
    best(a, "ACCEPTED");
    best(draft, "ACCEPTED");

    var json = body(call(BASE + "/topics/progress"));

    assertThat(json.path("solveStateAvailable").asBoolean()).isTrue();
    var rows = new java.util.HashMap<String, JsonNode>();
    json.path("items").forEach(i -> rows.put(i.path("topicId").asString(), i));
    assertThat(rows.get(arrays.toString()).path("total").asInt()).isEqualTo(2);
    assertThat(rows.get(arrays.toString()).path("solved").asInt()).isEqualTo(1);
    assertThat(rows.get(empty.toString()).path("total").asInt()).isZero();
    assertThat(rows.get(empty.toString()).path("solved").asInt()).isZero();
  }

  @Test
  void levelProgressSumsPerLevelAndKeepsLevelsWithoutProblems() throws Exception {
    var a = problem("A", "PUBLISHED", false, levelEasy);
    problem("B", "PUBLISHED", false, levelHard);
    problem("C", "UNPUBLISHED", false, levelHard);
    best(a, "ACCEPTED");

    var json = body(call(BASE + "/me/levels/progress"));

    int levels = jdbc.queryForObject("SELECT count(*) FROM problem.problem_levels", Integer.class);
    assertThat(json.path("byLevel")).hasSize(levels);
    assertThat(json.path("totalPublished").asInt()).isEqualTo(2);
    assertThat(json.path("solved").asInt()).isEqualTo(1);
    var sum = 0;
    for (var level : json.path("byLevel")) {
      sum += level.path("total").asInt();
    }
    assertThat(sum).isEqualTo(2);
    assertThat(json.path("byLevel").get(0).path("levelCode").asString()).isEqualTo(code(levelEasy));
  }

  @Test
  void theSummaryCountsVisibleProblemsPerLevelAndPerTopic() throws Exception {
    var graphs = topic("Graphs");
    var maths = topic("Maths");
    var a = problem("A", "PUBLISHED", false, levelEasy);
    var b = problem("B", "PUBLISHED", false, levelHard);
    var draft = problem("C", "UNPUBLISHED", false, levelHard);
    topic("Unused");
    link(a, graphs);
    link(a, maths);
    link(b, graphs);
    link(draft, maths);

    var json = body(call(BASE + "/catalog/summary"));

    assertThat(json.path("totalPublished").asInt()).isEqualTo(2);
    var perTopic = new java.util.HashMap<String, Integer>();
    json.path("byTopic")
        .forEach(t -> perTopic.put(t.path("topicId").asString(), t.path("count").asInt()));
    assertThat(perTopic).containsEntry(graphs.toString(), 2).containsEntry(maths.toString(), 1);
    assertThat(perTopic).hasSize(2);
    int levels = jdbc.queryForObject("SELECT count(*) FROM problem.problem_levels", Integer.class);
    assertThat(json.path("byLevel")).hasSize(levels);
    int total = 0;
    for (var level : json.path("byLevel")) {
      total += level.path("count").asInt();
    }
    assertThat(total).isEqualTo(2);
  }

  @Test
  void theBookmarkListIsNewestFirstKeepsWithdrawnAndOnlyShowsTheCallersOwn() throws Exception {
    var first = problem("A-FIRST", "PUBLISHED", false, levelEasy);
    var second = problem("B-SECOND", "PUBLISHED", false, levelHard);
    var withdrawn = problem("C-GONE", "UNPUBLISHED", false, levelEasy);
    mark(me, first, "first note", "2026-01-01T00:00:00Z");
    mark(me, second, null, "2026-01-02T00:00:00Z");
    mark(me, withdrawn, "old", "2026-01-03T00:00:00Z");
    mark(UUID.randomUUID(), first, "someone else", "2026-01-04T00:00:00Z");
    best(first, "ACCEPTED");

    var json = body(call(BASE + "/me/bookmarks", "userId", UUID.randomUUID().toString()));

    assertThat(json.path("items")).hasSize(3);
    assertThat(json.path("items").get(0).path("code").asString()).isEqualTo("C-GONE");
    assertThat(json.path("items").get(0).path("withdrawn").asBoolean()).isTrue();
    assertThat(json.path("items").get(1).path("note").asString()).isEmpty();
    assertThat(json.path("items").get(2).path("note").asString()).isEqualTo("first note");
    assertThat(json.path("items").get(2).path("solveState").asString()).isEqualTo("SOLVED");
    assertThat(json.path("meta").path("savedTotal").asInt()).isEqualTo(3);
    assertThat(json.path("meta").path("solvedCount").asInt()).isEqualTo(1);
    assertThat(response(call(BASE + "/me/bookmarks", "status", "SOLVED")).path("items")).hasSize(1);
    assertThat(response(call(BASE + "/me/bookmarks", "status", "UNSOLVED")).path("items"))
        .hasSize(2);
    assertThat(response(call(BASE + "/me/bookmarks", "difficulty", code(levelHard))).path("items"))
        .hasSize(1);
    assertThat(response(call(BASE + "/me/bookmarks", "q", "b-sec")).path("items")).hasSize(1);
    assertThat(
            response(call(BASE + "/me/bookmarks", "dir", "asc"))
                .path("items")
                .get(0)
                .path("code")
                .asString())
        .isEqualTo("A-FIRST");
  }

  @Test
  void twentyOneBookmarksPageAtTwentyAndALatePageIsTheLastPage() throws Exception {
    for (int i = 1; i <= 21; i++) {
      mark(
          me,
          problem(String.format("P%02d", i), "PUBLISHED", false, levelEasy),
          null,
          "2026-02-" + String.format("%02d", i) + "T00:00:00Z");
    }

    var one = body(call(BASE + "/me/bookmarks"));
    var late = body(call(BASE + "/me/bookmarks", "page", "9"));

    assertThat(one.path("items")).hasSize(20);
    assertThat(one.path("page").path("totalPages").asInt()).isEqualTo(2);
    assertThat(late.path("items")).hasSize(1);
    assertThat(late.path("page").path("number").asInt()).isEqualTo(2);
  }

  @Test
  void savingCreatesThenUpdatesKeepingTheSavedTime() throws Exception {
    var id = problem("SAVE", "PUBLISHED", false, levelEasy);

    var created = send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"  remember  \"}");
    assertThat(created.getStatus()).isEqualTo(201);
    var savedAt = body(created).path("savedAt").asString();
    assertThat(body(created).path("note").asString()).isEqualTo("remember");

    var updated = send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"changed\"}");
    assertThat(updated.getStatus()).isEqualTo(200);
    assertThat(body(updated).path("savedAt").asString()).isEqualTo(savedAt);
    assertThat(
            jdbc.queryForObject(
                "SELECT note FROM problem.bookmarks WHERE user_id = ? AND problem_id = ?",
                String.class,
                me,
                id))
        .isEqualTo("changed");
    var blank = send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"   \"}");
    assertThat(body(blank).path("note").isNull()).isTrue();
  }

  @Test
  void aNoteOverTheLimitIsRefusedAndAMissingBookmarkIsNotRecreatedQuietly() throws Exception {
    var id = problem("LIM", "PUBLISHED", false, levelEasy);

    assertThat(
            send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"" + "n".repeat(1001) + "\"}")
                .getStatus())
        .isEqualTo(400);
    assertThat(
            send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"" + "n".repeat(1000) + "\"}")
                .getStatus())
        .isEqualTo(201);
    assertThat(send(delete(BASE + "/me/bookmarks/" + id), "").getStatus()).isEqualTo(204);

    var missing =
        send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"x\",\"createIfMissing\":false}");
    assertThat(missing.getStatus()).isEqualTo(404);
    assertThat(body(missing).path("errorCode").asString()).isEqualTo("PRB-014");
    assertThat(count("problem.bookmarks WHERE user_id = '" + me + "'")).isZero();
    var undo =
        send(put(BASE + "/me/bookmarks/" + id), "{\"note\":\"back\",\"createIfMissing\":true}");
    assertThat(undo.getStatus()).isEqualTo(201);
  }

  @Test
  void aWithdrawnProblemCannotBeNewlySavedButAnExistingBookmarkStaysEditableAndRemovable()
      throws Exception {
    var draft = problem("DRAFT", "UNPUBLISHED", false, levelEasy);
    var kept = problem("KEPT", "UNPUBLISHED", false, levelEasy);
    mark(me, kept, "old", "2026-01-01T00:00:00Z");

    var refused = send(put(BASE + "/me/bookmarks/" + draft), "{\"note\":\"x\"}");
    assertThat(refused.getStatus()).isEqualTo(404);
    assertThat(body(refused).path("errorCode").asString()).isEqualTo("PRB-010");
    assertThat(send(put(BASE + "/me/bookmarks/" + kept), "{\"note\":\"edited\"}").getStatus())
        .isEqualTo(200);
    assertThat(send(delete(BASE + "/me/bookmarks/" + kept), "").getStatus()).isEqualTo(204);
    assertThat(send(delete(BASE + "/me/bookmarks/" + kept), "").getStatus()).isEqualTo(204);
  }

  @Test
  void theStateEndpointSetsTheWantedStateAndNeverDuplicatesOrLosesTheNote() throws Exception {
    var id = problem("STATE", "PUBLISHED", false, levelEasy);
    var path = BASE + "/me/bookmarks/" + id + "/state";

    assertThat(body(send(put(path), "{\"bookmarked\":true}")).path("bookmarked").asBoolean())
        .isTrue();
    jdbc.update(
        "UPDATE problem.bookmarks SET note = 'kept' WHERE user_id = ? AND problem_id = ?", me, id);
    assertThat(send(put(path), "{\"bookmarked\":true}").getStatus()).isEqualTo(200);
    assertThat(
            count("problem.bookmarks WHERE user_id = '" + me + "' AND problem_id = '" + id + "'"))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT note FROM problem.bookmarks WHERE user_id = ? AND problem_id = ?",
                String.class,
                me,
                id))
        .isEqualTo("kept");
    assertThat(body(call(BASE + "/problems/" + id + "/workspace")).path("bookmarked").asBoolean())
        .isTrue();
    assertThat(body(send(put(path), "{\"bookmarked\":false}")).path("bookmarked").asBoolean())
        .isFalse();
    assertThat(send(put(path), "{\"bookmarked\":false}").getStatus()).isEqualTo(200);
    assertThat(count("problem.bookmarks WHERE user_id = '" + me + "'")).isZero();
  }

  @Test
  void theStateEndpointRefusesABadBodyAndAnUnpublishedProblem() throws Exception {
    var draft = problem("DRAFT", "UNPUBLISHED", false, levelEasy);

    assertThat(send(put(BASE + "/me/bookmarks/" + draft + "/state"), "{}").getStatus())
        .isEqualTo(400);
    assertThat(
            send(put(BASE + "/me/bookmarks/" + draft + "/state"), "{\"bookmarked\":\"yes\"}")
                .getStatus())
        .isEqualTo(400);
    var refused = send(put(BASE + "/me/bookmarks/" + draft + "/state"), "{\"bookmarked\":true}");
    assertThat(refused.getStatus()).isEqualTo(404);
    assertThat(body(refused).path("errorCode").asString()).isEqualTo("PRB-010");
  }

  @Test
  void aCallWithoutATokenIs401() throws Exception {
    for (String path :
        List.of("/topics/progress", "/me/levels/progress", "/catalog/summary", "/me/bookmarks")) {
      assertThat(mvc.perform(get(BASE + path)).andReturn().getResponse().getStatus())
          .as(path)
          .isEqualTo(401);
    }
  }

  // ---- extra helpers

  private MockHttpServletResponse send(MockHttpServletRequestBuilder request, String json)
      throws Exception {
    request.header("Authorization", "Bearer " + token);
    if (!json.isEmpty()) {
      request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private JsonNode response(MockHttpServletResponse response) throws Exception {
    return body(response);
  }

  private void mark(UUID user, UUID problem, String note, String at) {
    jdbc.update(
        "INSERT INTO problem.bookmarks(user_id,problem_id,note,created_at,updated_at) VALUES (?,?,?,?::timestamptz,?::timestamptz)",
        user,
        problem,
        note,
        at,
        at);
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
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
