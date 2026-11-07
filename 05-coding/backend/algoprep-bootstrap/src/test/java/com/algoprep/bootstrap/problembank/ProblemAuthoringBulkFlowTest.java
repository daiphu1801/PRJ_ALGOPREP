package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.nio.charset.StandardCharsets;
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

/** Export #27 and bulk actions #28 to #32 over HTTP with real PostgreSQL, MinIO and permissions. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemAuthoringBulkFlowTest {
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

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-bulk-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-bulk-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-bulk-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-bulk-password");
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
  private UUID rivalId;
  private String teacher;
  private String rival;
  private String admin;
  private String student;
  private List<UUID> levels;
  private List<UUID> topics;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    reset(runner);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    levels =
        jdbc.queryForList("SELECT id FROM problem.problem_levels ORDER BY sort_order", UUID.class);
    topics = jdbc.queryForList("SELECT id FROM problem.topics ORDER BY sort_order", UUID.class);
    teacherId = UUID.randomUUID();
    rivalId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(rivalId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  // ---- export

  @Test
  void exportOfSelectedIdsHasAHeaderEightColumnsAndDefusesFormulas() throws Exception {
    var a = problem(teacherId, "Two Sum", "PUBLISHED");
    var b = problem(teacherId, "=SUM(1,1)", "UNPUBLISHED");
    var foreign = problem(rivalId, "Rival", "PUBLISHED");

    var response =
        call(
            "/export",
            teacher,
            "{\"problemIds\":[\"" + a + "\",\"" + b + "\",\"" + foreign + "\"]}");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).startsWith("text/csv");
    assertThat(response.getHeader("Content-Disposition"))
        .contains("attachment")
        .containsPattern("problems-\\d{8}\\.csv");
    String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
    assertThat(body)
        .startsWith("﻿Mã,Tiêu đề,Chủ đề,Độ khó,Trạng thái,Lượt nộp,Tỉ lệ AC,Số testcase");
    var lines = body.split("\r\n");
    assertThat(lines)
        .hasSize(3); // header + the two own problems; the other author's id is silently absent
    assertThat(body).contains("'=SUM(1,1)").doesNotContain("Rival");
    assertThat(lines[0].split(",", -1)).hasSize(8);
  }

  @Test
  void exportByFilterAndTheExactlyOneRule() throws Exception {
    problem(teacherId, "Alpha Unique", "PUBLISHED");
    problem(teacherId, "Beta", "UNPUBLISHED");

    var filtered = call("/export", teacher, "{\"filter\":{\"q\":\"Alpha Unique\"}}");
    assertThat(filtered.getStatus()).isEqualTo(200);
    assertThat(filtered.getContentAsString(StandardCharsets.UTF_8))
        .contains("Alpha Unique")
        .doesNotContain("Beta");

    for (String bad :
        new String[] {
          "{}",
          "{\"problemIds\":[],\"filter\":null}",
          "{\"problemIds\":[\"" + UUID.randomUUID() + "\"],\"filter\":{}}"
        }) {
      var response = call("/export", teacher, bad);
      assertThat(response.getStatus()).as(bad).isEqualTo(400);
      assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-001");
    }
    assertThat(call("/export", student, "{\"filter\":{}}").getStatus()).isEqualTo(403);
  }

  // ---- difficulty and topic

  @Test
  void difficultyChangesEachProblemAuditsItAndSkipsOnesAlreadyThere() throws Exception {
    var one = problem(teacherId, "One", "UNPUBLISHED");
    var two = problem(teacherId, "Two", "UNPUBLISHED");
    var already = problem(teacherId, "Already", "UNPUBLISHED");
    UUID hard = levels.get(levels.size() - 1);
    jdbc.update("UPDATE problem.problems SET level_id = ? WHERE id = ?", hard, already);

    var result =
        json(
            call(
                "/bulk/difficulty",
                teacher,
                ids(one, two, already) + ",\"levelId\":\"" + hard + "\"}"));

    assertThat(result.path("succeededIds")).hasSize(3);
    assertThat(result.path("failedItems")).isEmpty();
    for (var id : List.of(one, two, already)) {
      assertThat(
              jdbc.queryForObject(
                  "SELECT level_id FROM problem.problems WHERE id = ?", UUID.class, id))
          .isEqualTo(hard);
    }
    assertThat(auditCount("PROBLEM_DIFFICULTY_CHANGED", one, two, already))
        .isEqualTo(2); // none for the one already there
  }

  @Test
  void anUnknownLevelOrTopicRefusesTheWholeBatchAndChangesNothing() throws Exception {
    var id = problem(teacherId, "Keep", "UNPUBLISHED");
    UUID before =
        jdbc.queryForObject("SELECT level_id FROM problem.problems WHERE id = ?", UUID.class, id);

    var level =
        call("/bulk/difficulty", teacher, ids(id) + ",\"levelId\":\"" + UUID.randomUUID() + "\"}");
    var topic =
        call("/bulk/topic", teacher, ids(id) + ",\"topicId\":\"" + UUID.randomUUID() + "\"}");

    assertThat(level.getStatus()).isEqualTo(422);
    assertThat(json(level).path("errorCode").asString()).isEqualTo("PRB-039");
    assertThat(topic.getStatus()).isEqualTo(422);
    assertThat(
            jdbc.queryForObject(
                "SELECT level_id FROM problem.problems WHERE id = ?", UUID.class, id))
        .isEqualTo(before);
    assertThat(call("/bulk/difficulty", teacher, ids(id) + "}").getStatus())
        .isEqualTo(400); // levelId missing
  }

  @Test
  void topicIsAddedOnceAndAnExistingLinkIsNeitherDuplicatedNorAudited() throws Exception {
    var fresh = problem(teacherId, "Fresh", "UNPUBLISHED");
    var linked = problem(teacherId, "Linked", "UNPUBLISHED");
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
        linked,
        topics.get(0));

    var result =
        json(
            call(
                "/bulk/topic",
                teacher,
                ids(fresh, linked) + ",\"topicId\":\"" + topics.get(0) + "\"}"));

    assertThat(result.path("succeededIds")).hasSize(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.problem_topics WHERE topic_id = ? AND problem_id IN (?, ?)",
                Integer.class,
                topics.get(0),
                fresh,
                linked))
        .isEqualTo(2);
    assertThat(auditCount("PROBLEM_TOPIC_ASSIGNED", fresh, linked)).isEqualTo(1);
  }

  // ---- delete

  @Test
  void deleteIsSoftKeepsEverythingElseAndWithdrawsAPublishedProblem() throws Exception {
    var id = problem(teacherId, "To Delete", "PUBLISHED");
    testcase(id);
    jdbc.update(
        "INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (?, ?)",
        id,
        topics.get(0));
    jdbc.update(
        "INSERT INTO problem.class_assignments(id, problem_id, class_id, assigned_by) VALUES (?,?,?,?)",
        UUID.randomUUID(),
        id,
        UUID.randomUUID(),
        teacherId);

    var result = json(call("/bulk/delete", teacher, ids(id) + "}"));

    assertThat(result.path("succeededIds")).hasSize(1);
    var row = jdbc.queryForMap("SELECT deleted, status FROM problem.problems WHERE id = ?", id);
    assertThat(row.get("deleted")).isEqualTo(true);
    assertThat(row.get("status")).isEqualTo("UNPUBLISHED");
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(1);
    assertThat(count("problem.problem_topics WHERE problem_id = '" + id + "'")).isEqualTo(1);
    assertThat(count("problem.class_assignments WHERE problem_id = '" + id + "'")).isEqualTo(1);
    assertThat(
            count(
                "problem.outbox_events WHERE aggregate_id = '"
                    + id
                    + "' AND event_type = 'ProblemUnpublished'"))
        .isEqualTo(1);
    assertThat(auditCount("PROBLEM_DELETED", id)).isEqualTo(1);
    // The same call again: the problem is gone for the screen.
    var again = json(call("/bulk/delete", teacher, ids(id) + "}"));
    assertThat(again.path("failedItems").get(0).path("code").asString()).isEqualTo("PRB-010");
  }

  @Test
  void aBatchMixesSuccessAndFailureAndNeverAbortsOnTheSecondProblem() throws Exception {
    var mine = problem(teacherId, "Mine", "UNPUBLISHED");
    var theirsPublished = problem(rivalId, "Their Published", "PUBLISHED");
    var theirsDraft = problem(rivalId, "Their Draft", "UNPUBLISHED");

    var result =
        json(
            call(
                "/bulk/delete",
                teacher,
                ids(mine, theirsPublished, theirsDraft, UUID.randomUUID()) + "}"));

    assertThat(result.path("succeededIds")).hasSize(1);
    var codes = new ArrayList<String>();
    result.path("failedItems").forEach(f -> codes.add(f.path("code").asString()));
    assertThat(codes)
        .containsExactly(
            "PRB-003", "PRB-010", "PRB-010"); // another author's draft looks like a missing id
    assertThat(
            jdbc.queryForObject(
                "SELECT deleted FROM problem.problems WHERE id = ?",
                Boolean.class,
                theirsPublished))
        .isFalse();
    // An admin is not bound to authorship.
    assertThat(json(call("/bulk/delete", admin, ids(theirsPublished) + "}")).path("succeededIds"))
        .hasSize(1);
  }

  // ---- unpublish and publish

  @Test
  void unpublishIsIdempotentAndAuditsOnlyARealChange() throws Exception {
    var published = problem(teacherId, "Was Published", "PUBLISHED");
    var draft = problem(teacherId, "Was Draft", "UNPUBLISHED");

    var result = json(call("/bulk/unpublish", teacher, ids(published, draft) + "}"));

    assertThat(result.path("succeededIds")).hasSize(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM problem.problems WHERE id = ?", String.class, published))
        .isEqualTo("UNPUBLISHED");
    assertThat(auditCount("PROBLEM_UNPUBLISHED", published, draft)).isEqualTo(1);
  }

  @Test
  void publishMixesAnEligibleProblemAndAShortOneAndCapsTheBatchAtTwenty() throws Exception {
    var complete = completeProblem();
    var shortOne = problem(teacherId, "Too Short", "UNPUBLISHED");
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

    var result = json(call("/bulk/publish", teacher, ids(complete, shortOne) + "}"));

    assertThat(result.path("succeededIds")).hasSize(1);
    assertThat(result.path("succeededIds").get(0).asString()).isEqualTo(complete.toString());
    var failed = result.path("failedItems").get(0);
    assertThat(failed.path("code").asString()).isEqualTo("PRB-030");
    assertThat(failed.path("details").path("items").size()).isPositive();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM problem.problems WHERE id = ?", String.class, shortOne))
        .isEqualTo("UNPUBLISHED");

    var over = new UUID[21];
    for (int i = 0; i < over.length; i++) {
      over[i] = UUID.randomUUID();
    }
    assertThat(call("/bulk/publish", teacher, ids(over) + "}").getStatus()).isEqualTo(400);
  }

  @Test
  void permissionsAndBadBodiesAreRefusedForTheWholeRequest() throws Exception {
    var id = problem(teacherId, "Any", "UNPUBLISHED");

    assertThat(call("/bulk/delete", student, ids(id) + "}").getStatus()).isEqualTo(403);
    assertThat(
            call("/bulk/topic", student, ids(id) + ",\"topicId\":\"" + topics.get(0) + "\"}")
                .getStatus())
        .isEqualTo(403);
    assertThat(call("/bulk/delete", teacher, "{\"problemIds\":[]}").getStatus()).isEqualTo(400);
    assertThat(call("/bulk/delete", teacher, "{\"problemIds\":[\"not-a-uuid\"]}").getStatus())
        .isEqualTo(400);
    assertThat(call("/bulk/unpublish", rival, ids(id) + "}").getStatus())
        .isEqualTo(200); // refused per item
    assertThat(
            json(call("/bulk/unpublish", rival, ids(id) + "}"))
                .path("failedItems")
                .get(0)
                .path("code")
                .asString())
        .isEqualTo("PRB-010");
  }

  // ---- helpers

  private MockHttpServletResponse call(String path, String token, String body) throws Exception {
    return mvc.perform(
            post(BASE + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  /** The opening of a bulk body: {@code {"problemIds":[...]}} without the closing brace. */
  private static String ids(UUID... ids) {
    var parts = new ArrayList<String>();
    for (UUID id : ids) {
      parts.add("\"" + id + "\"");
    }
    return "{\"problemIds\":[" + String.join(",", parts) + "]";
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Bulk actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID problem(UUID author, String title, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'s',?,?,?,1000,256,64)",
        id,
        id.toString(),
        title,
        author,
        levels.get(0),
        status);
    return id;
  }

  private void testcase(UUID problem) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','i','INLINE','o','MANUAL',true,1)",
        UUID.randomUUID(),
        problem);
  }

  /** A problem that satisfies all five publish conditions once the sandbox answers "pass". */
  private UUID completeProblem() {
    var id = problem(teacherId, "Complete", "UNPUBLISHED");
    for (int i = 1; i <= 2; i++) {
      jdbc.update(
          "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          i,
          "i",
          "o");
    }
    for (int i = 1; i <= 8; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
          UUID.randomUUID(),
          id,
          i <= 2 ? "SAMPLE" : "HIDDEN",
          "in" + i,
          "out" + i,
          i);
    }
    jdbc.update(
        "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON','x')",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'in','out','f','{\"kind\":\"INT\"}'::jsonb,'[]'::jsonb)",
        UUID.randomUUID(),
        id);
    return id;
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
  }

  private int auditCount(String action, UUID... targets) {
    int total = 0;
    for (UUID target : targets) {
      total +=
          count(
              "identity.system_audit_logs WHERE action_type = '"
                  + action
                  + "' AND target_id = '"
                  + target
                  + "'");
    }
    return total;
  }
}
