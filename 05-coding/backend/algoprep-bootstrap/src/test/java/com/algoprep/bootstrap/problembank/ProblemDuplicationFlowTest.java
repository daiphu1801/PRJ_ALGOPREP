package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
 * #34 (duplicate source) and #35 with {@code duplicatedFromProblemId} (DD F2 sections 35 and 36.5)
 * over HTTP with real PostgreSQL and MinIO: the spec and every testcase are copied, MinIO objects
 * under new keys, and the source stays untouched.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemDuplicationFlowTest {
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
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-testcase-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-testcase-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

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
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  // ---- #34 duplicate source

  @Test
  void theSourceIsReturnedAsAPrefillWithoutIdsAndNothingIsWritten() throws Exception {
    var id = source("UNPUBLISHED");
    int problems = count("problem.problems");
    int cases = count("problem.testcases");

    var response = send(get(BASE + "/" + id + "/duplicate-source"), teacher, null);

    assertThat(response.getStatus()).isEqualTo(200);
    var json = json(response);
    assertThat(json.path("title").asString()).isEqualTo("Source problem");
    assertThat(json.path("duplicatedFromProblemId").asString()).isEqualTo(id.toString());
    assertThat(json.path("limits").path("timeLimitMs").asInt()).isEqualTo(1000);
    assertThat(json.path("referenceSolution").path("language").asString()).isEqualTo("PYTHON");
    assertThat(json.path("workedExamples")).hasSize(1);
    assertThat(json.path("workedExamples").get(0).path("id").isNull()).isTrue();
    assertThat(json.path("willCopyOnSave").path("testcaseCount").asInt()).isEqualTo(2);
    assertThat(json.path("willCopyOnSave").path("hasSpec").asBoolean()).isTrue();
    assertThat(json.has("id")).isFalse();
    assertThat(json.has("testcases")).isFalse();
    assertThat(count("problem.problems")).isEqualTo(problems);
    assertThat(count("problem.testcases")).isEqualTo(cases);
  }

  @Test
  void aSourceWithoutSpecOrAnswerReportsNoSpec() throws Exception {
    var id = bare();

    var json = json(send(get(BASE + "/" + id + "/duplicate-source"), teacher, null));

    assertThat(json.path("willCopyOnSave").path("hasSpec").asBoolean()).isFalse();
    assertThat(json.path("willCopyOnSave").path("testcaseCount").asInt()).isZero();
    assertThat(json.path("referenceSolution").isNull()).isTrue();
  }

  @Test
  void duplicateSourceIsRefusedForAnotherAuthorAStudentAndMissingOrDeletedProblems()
      throws Exception {
    var id = source("UNPUBLISHED");
    var deleted = bare();
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", deleted);
    String path = BASE + "/" + id + "/duplicate-source";

    var other = send(get(path), rival, null);
    assertThat(other.getStatus()).isEqualTo(403);
    assertThat(json(other).path("errorCode").asString()).isEqualTo("PRB-003");
    var denied = send(get(path), student, null);
    assertThat(denied.getStatus()).isEqualTo(403);
    assertThat(json(denied).path("errorCode").asString()).isEqualTo("PRB-002");
    assertThat(send(get(BASE + "/" + deleted + "/duplicate-source"), teacher, null).getStatus())
        .isEqualTo(404);
    assertThat(
            send(get(BASE + "/" + UUID.randomUUID() + "/duplicate-source"), teacher, null)
                .getStatus())
        .isEqualTo(404);
    assertThat(send(get(BASE + "/not-a-uuid/duplicate-source"), teacher, null).getStatus())
        .isEqualTo(404);
  }

  // ---- #35 with duplicatedFromProblemId

  @Test
  void aDuplicateCopiesTheSpecAtVersionOneAndEveryTestcaseWithFreshObjects() throws Exception {
    var id = source("PUBLISHED");
    var sourceKey =
        jdbc.queryForObject(
            "SELECT input_object_key FROM problem.testcases WHERE problem_id=? AND input_storage='MINIO'",
            String.class,
            id);

    var response = send(post(BASE), teacher, create("Copy of source", id.toString()));

    assertThat(response.getStatus()).isEqualTo(201);
    var json = json(response);
    var copy = UUID.fromString(json.path("id").asString());
    assertThat(copy).isNotEqualTo(id);
    assertThat(json.path("status").asString()).isEqualTo("UNPUBLISHED");
    assertThat(json.path("duplicatedFromProblemId").asString()).isEqualTo(id.toString());
    assertThat(json.path("spec").path("specVersion").asInt()).isEqualTo(1);
    assertThat(json.path("spec").path("functionName").asString()).isEqualTo("solve");
    assertThat(json.path("currentTestcaseSetVersion").asInt()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT duplicated_from_problem_id FROM problem.problems WHERE id=?",
                UUID.class,
                copy))
        .isEqualTo(id);
    assertThat(
            jdbc.queryForObject(
                "SELECT spec_version FROM problem.problem_specs WHERE problem_id=?",
                Integer.class,
                copy))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT spec_version FROM problem.problem_specs WHERE problem_id=?",
                Integer.class,
                id))
        .isEqualTo(7);

    assertCopiedTestcases(copy, id, sourceKey);
  }

  private void assertCopiedTestcases(UUID copy, UUID id, String sourceKey) throws Exception {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM problem.testcases WHERE problem_id=? ORDER BY position", copy);
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).get("visibility")).isEqualTo("SAMPLE");
    assertThat(rows.get(0).get("approved")).isEqualTo(true);
    assertThat(rows.get(0).get("input_inline")).isEqualTo("in1");
    var minioRow = rows.get(1);
    assertThat(minioRow.get("category")).isEqualTo("EXTREME");
    var copyKey = (String) minioRow.get("input_object_key");
    assertThat(copyKey)
        .startsWith("problems/" + copy + "/testcases/" + minioRow.get("id") + "/input-")
        .isNotEqualTo(sourceKey);
    assertThat(read(copyKey)).isEqualTo(read(sourceKey)).isEqualTo("x".repeat(9000));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.testcases WHERE problem_id=?", Integer.class, id))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs WHERE action_type='PROBLEM_DUPLICATED' AND target_id=?",
                Integer.class,
                copy.toString()))
        .isEqualTo(1);
  }

  @Test
  void aDuplicateOfAnotherAuthorsProblemIsForbiddenAndWritesNothing() throws Exception {
    var id = source("UNPUBLISHED");
    int before = count("problem.problems");

    var response = send(post(BASE), rival, create("Stolen", id.toString()));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-003");
    assertThat(count("problem.problems")).isEqualTo(before);
  }

  @Test
  void aDeletedOrMissingSourceIs404AndWritesNothing() throws Exception {
    var deleted = bare();
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", deleted);
    int before = count("problem.problems");

    assertThat(send(post(BASE), teacher, create("Gone", deleted.toString())).getStatus())
        .isEqualTo(404);
    assertThat(send(post(BASE), teacher, create("Never", UUID.randomUUID().toString())).getStatus())
        .isEqualTo(404);
    assertThat(count("problem.problems")).isEqualTo(before);
  }

  @Test
  void aSourceObjectThatIsGoneFailsTheWholeCreateAndLeavesNoRow() throws Exception {
    var id = source("UNPUBLISHED");
    var key =
        jdbc.queryForObject(
            "SELECT input_object_key FROM problem.testcases WHERE problem_id=? AND input_storage='MINIO'",
            String.class,
            id);
    minio.removeObject(io.minio.RemoveObjectArgs.builder().bucket(BUCKET).object(key).build());
    int before = count("problem.problems");

    var response = send(post(BASE), teacher, create("Broken copy", id.toString()));

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-052");
    assertThat(count("problem.problems")).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.testcases WHERE input_object_key LIKE ?",
                Integer.class,
                "problems/" + id + "/%"))
        .isEqualTo(1);
  }

  // ---- helpers

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String token, String body) throws Exception {
    request.header("Authorization", "Bearer " + token);
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static String create(String title, String source) {
    return "{\"title\":\""
        + title
        + "\",\"statementMd\":\"statement\",\"levelId\":\""
        + LEVEL
        + "\",\"duplicatedFromProblemId\":\""
        + source
        + "\"}";
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Duplicate actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID bare() {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Bare problem','statement',?,?::uuid,'UNPUBLISHED',1000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL);
    return id;
  }

  /** A teacher's problem with a spec at version 7, an example, an answer and two testcases. */
  private UUID source(String status) throws Exception {
    var id = bare();
    jdbc.update(
        "UPDATE problem.problems SET title='Source problem', status=? WHERE id=?", status, id);
    jdbc.update(
        "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,1,'1','2')",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON','print(1)')",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters,spec_version) VALUES (?,?,'in','out','solve','{\"kind\":\"INT\"}'::jsonb,'[]'::jsonb,7)",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'SAMPLE','INLINE','in1','INLINE','out1','MANUAL',true,1)",
        UUID.randomUUID(),
        id);
    var testcase = UUID.randomUUID();
    var key = "problems/" + id + "/testcases/" + testcase + "/input-" + UUID.randomUUID() + ".txt";
    var bytes = "x".repeat(9000).getBytes(StandardCharsets.UTF_8);
    minio.putObject(
        PutObjectArgs.builder().bucket(BUCKET).object(key).stream(
                new ByteArrayInputStream(bytes), bytes.length, -1)
            .build());
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_object_key,expected_output_storage,expected_output_inline,origin,approved,category,position) VALUES (?,?,'HIDDEN','MINIO',?,'INLINE','out2','MANUAL',true,'EXTREME',2)",
        testcase,
        id,
        key);
    return id;
  }

  private String read(String key) throws Exception {
    try (var stream = minio.getObject(GetObjectArgs.builder().bucket(BUCKET).object(key).build())) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }
}
