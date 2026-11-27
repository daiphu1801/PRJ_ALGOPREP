package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
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

/**
 * Authoring #33, #35 and #36 over HTTP with real JWT permissions and PostgreSQL (AC-F2 authoring).
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemAuthoringHttpFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/authoring/problems";
  private static final String LEVEL = "20000000-0000-4000-8000-000000000001";

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
  private String teacher;
  private String rival;
  private String admin;
  private String student;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  @Test
  void createFillsMissingLimitsFromTheDefaultsAndLeavesAnUnpublishedDraft() throws Exception {
    var response = send(post(BASE), teacher, body("Two Sum", null));

    assertThat(response.getStatus()).isEqualTo(201);
    var json = json(response);
    assertThat(json.path("status").asString()).isEqualTo("UNPUBLISHED");
    assertThat(json.path("code").asString()).startsWith("TWO-SUM");
    assertThat(json.path("authorId").asString()).isEqualTo(teacherId.toString());
    assertThat(json.path("readOnly").asBoolean()).isFalse();
    var defaults =
        jdbc.queryForMap("SELECT time_limit_ms AS t FROM judge.grading_defaults WHERE id = 1");
    assertThat(json.path("limits").path("timeLimitMs").asInt())
        .isEqualTo(((Number) defaults.get("t")).intValue());
    assertThat(json.path("limits").path("maxSubmissionsPerHour").isNull()).isTrue();
    assertThat(json.path("checklist").path("items")).hasSize(5);
  }

  @Test
  void createWithAnUnknownDuplicateSourceIs404AndWritesNothing() throws Exception {
    int before = jdbc.queryForObject("SELECT count(*) FROM problem.problems", Integer.class);

    var response =
        send(
            post(BASE),
            teacher,
            body("Dup", "\"duplicatedFromProblemId\":\"" + UUID.randomUUID() + "\""));

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-010");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problems", Integer.class))
        .isEqualTo(before);
  }

  @Test
  void createRejectsBadInputAndMissingPermission() throws Exception {
    var empty = send(post(BASE), teacher, body("   ", null));
    assertThat(empty.getStatus()).isEqualTo(400);
    assertThat(json(empty).path("errorCode").asString()).isEqualTo("PRB-001");

    var gone =
        send(post(BASE), teacher, body("Gone level", "\"levelId\":\"" + UUID.randomUUID() + "\""));
    assertThat(gone.getStatus()).isEqualTo(422);
    assertThat(json(gone).path("errorCode").asString()).isEqualTo("PRB-039");

    var denied = send(post(BASE), student, body("Nope", null));
    assertThat(denied.getStatus()).isEqualTo(403);
    assertThat(json(denied).path("errorCode").asString()).isEqualTo("PRB-002");

    var garbage = send(post(BASE), teacher, "{not json");
    assertThat(garbage.getStatus()).isEqualTo(400);
    assertThat(json(garbage).path("errorCode").asString()).isEqualTo("PRB-001");
  }

  @Test
  void authorReadsTestcasesWhileAStrangerCannotSeeADraftOrBodiesOfAPublishedOne() throws Exception {
    var id = problem(teacherId, "UNPUBLISHED");
    testcase(id, "SAMPLE", "1 2", "3");
    testcase(id, "HIDDEN", "secret-in", "secret-out");

    var own = json(send(get(BASE + "/" + id), teacher, null));
    assertThat(own.path("testcases")).hasSize(2);
    assertThat(own.path("testcases").get(1).path("input").asString()).isEqualTo("secret-in");
    assertThat(own.path("testcaseSummary").path("hidden").asInt()).isEqualTo(1);
    assertThat(own.path("aiGeneration").path("limit").asInt()).isPositive();

    var draft = send(get(BASE + "/" + id), rival, null);
    assertThat(draft.getStatus()).isEqualTo(404);
    assertThat(json(draft).path("errorCode").asString()).isEqualTo("PRB-010");

    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", id);
    var published = send(get(BASE + "/" + id), rival, null);
    assertThat(published.getStatus()).isEqualTo(200);
    var view = json(published);
    assertThat(view.path("readOnly").asBoolean()).isTrue();
    assertThat(view.path("testcases")).isEmpty();
    assertThat(view.path("referenceSolution").isNull()).isTrue();
    assertThat(published.getContentAsString())
        .doesNotContain("secret-in")
        .doesNotContain("secret-out");
  }

  @Test
  void adminReadsAnyDraftAndStudentGets403AndBadOrDeletedIdsAre404() throws Exception {
    var id = problem(teacherId, "UNPUBLISHED");
    assertThat(send(get(BASE + "/" + id), admin, null).getStatus()).isEqualTo(200);
    assertThat(send(get(BASE + "/" + id), student, null).getStatus()).isEqualTo(403);
    assertThat(send(get(BASE + "/not-a-uuid"), teacher, null).getStatus()).isEqualTo(404);
    assertThat(send(get(BASE + "/" + UUID.randomUUID()), teacher, null).getStatus()).isEqualTo(404);
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", id);
    assertThat(send(get(BASE + "/" + id), admin, null).getStatus()).isEqualTo(404);
  }

  @Test
  void updateReplacesContentAndAStaleTimestampIs409WithTheCurrentOne() throws Exception {
    var id = problem(teacherId, "UNPUBLISHED");
    var current = send(get(BASE + "/" + id), teacher, null);
    var stamp = json(current).path("updatedAt").asString();

    var saved =
        send(
            put(BASE + "/" + id + "/content"),
            teacher,
            update(
                "New title",
                stamp,
                "\"limits\":{\"timeLimitMs\":900,\"memoryLimitMb\":128,\"maxOutputSizeKb\":16}"));

    assertThat(saved.getStatus()).isEqualTo(200);
    var after = json(saved);
    assertThat(after.path("title").asString()).isEqualTo("New title");
    assertThat(after.path("limits").path("timeLimitMs").asInt()).isEqualTo(900);
    assertThat(Instant.parse(after.path("updatedAt").asString())).isAfter(Instant.parse(stamp));

    var stale = send(put(BASE + "/" + id + "/content"), teacher, update("Lost write", stamp, null));
    assertThat(stale.getStatus()).isEqualTo(409);
    var error = json(stale);
    assertThat(error.path("errorCode").asString()).isEqualTo("PRB-020");
    assertThat(error.path("details").path("currentUpdatedAt").asString()).isNotBlank();
    assertThat(title(id)).isEqualTo("New title");
  }

  @Test
  void updateOfSomeoneElsesProblemIs403WhenPublishedAnd404WhenDraft() throws Exception {
    var draft = problem(teacherId, "UNPUBLISHED");
    var published = problem(teacherId, "PUBLISHED");
    var stampDraft = stamp(draft);
    var stampPublished = stamp(published);

    var onDraft =
        send(put(BASE + "/" + draft + "/content"), rival, update("Hijack", stampDraft, null));
    var onPublished =
        send(
            put(BASE + "/" + published + "/content"),
            rival,
            update("Hijack", stampPublished, null));

    assertThat(onDraft.getStatus()).isEqualTo(404);
    assertThat(json(onPublished).path("errorCode").asString()).isEqualTo("PRB-003");
    assertThat(onPublished.getStatus()).isEqualTo(403);
    assertThat(title(draft)).isEqualTo("Fixture");
    assertThat(title(published)).isEqualTo("Fixture");
    // The admin is not bound to authorship.
    assertThat(
            send(put(BASE + "/" + draft + "/content"), admin, update("By admin", stampDraft, null))
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void updateOfAPublishedProblemKeepsTwoWorkedExamplesAndNeedsEveryLimit() throws Exception {
    var id = problem(teacherId, "PUBLISHED");
    var stamp = stamp(id);

    var oneExample =
        send(
            put(BASE + "/" + id + "/content"),
            teacher,
            update("Fewer", stamp, "\"workedExamples\":[{\"input\":\"1\",\"output\":\"1\"}]"));
    assertThat(oneExample.getStatus()).isEqualTo(422);
    assertThat(json(oneExample).path("errorCode").asString()).isEqualTo("PRB-053");

    var noLimits =
        send(
            put(BASE + "/" + id + "/content"),
            teacher,
            "{\"title\":\"x\",\"statementMd\":\"s\",\"expectedUpdatedAt\":\"" + stamp + "\"}");
    assertThat(noLimits.getStatus()).isEqualTo(400);
    assertThat(json(noLimits).path("errorCode").asString()).isEqualTo("PRB-001");
    assertThat(title(id)).isEqualTo("Fixture");
  }

  // ---- helpers

  private MockHttpServletResponse send(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String token,
      String body)
      throws Exception {
    request.header("Authorization", "Bearer " + token);
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  /** Create body; an {@code extra} fragment naming levelId replaces the default level. */
  private static String body(String title, String extra) {
    boolean ownLevel = extra != null && extra.contains("levelId");
    return "{\"title\":\""
        + title
        + "\",\"statementMd\":\"Find two numbers.\""
        + (ownLevel ? "" : ",\"levelId\":\"" + LEVEL + "\"")
        + (extra == null ? "" : "," + extra)
        + "}";
  }

  private static String update(String title, String stamp, String extra) {
    boolean hasLimits = extra != null && extra.contains("limits");
    boolean hasExamples = extra != null && extra.contains("workedExamples");
    return "{\"title\":\""
        + title
        + "\",\"statementMd\":\"Statement\",\"levelId\":\""
        + LEVEL
        + "\",\"expectedUpdatedAt\":\""
        + stamp
        + "\""
        + (hasLimits
            ? ""
            : ",\"limits\":{\"timeLimitMs\":1000,\"memoryLimitMb\":256,\"maxOutputSizeKb\":64}")
        + (hasExamples
            ? ""
            : ",\"workedExamples\":[{\"input\":\"1\",\"output\":\"1\"},{\"input\":\"2\",\"output\":\"2\"}]")
        + (extra == null ? "" : "," + extra)
        + "}";
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Author actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID problem(UUID author, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Fixture','statement',?,?::uuid,?,1000,256,64)",
        id,
        id.toString(),
        author,
        LEVEL,
        status);
    for (int i = 1; i <= 2; i++) {
      jdbc.update(
          "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          i,
          "i" + i,
          "o" + i);
    }
    return id;
  }

  private void testcase(UUID problem, String visibility, String input, String output) {
    int position =
        jdbc.queryForObject(
            "SELECT count(*) + 1 FROM problem.testcases WHERE problem_id=?",
            Integer.class,
            problem);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
        UUID.randomUUID(),
        problem,
        visibility,
        input,
        output,
        position);
  }

  private String title(UUID id) {
    return jdbc.queryForObject("SELECT title FROM problem.problems WHERE id=?", String.class, id);
  }

  private String stamp(UUID id) {
    return jdbc.queryForObject(
            "SELECT updated_at FROM problem.problems WHERE id=?", Timestamp.class, id)
        .toInstant()
        .toString();
  }
}
