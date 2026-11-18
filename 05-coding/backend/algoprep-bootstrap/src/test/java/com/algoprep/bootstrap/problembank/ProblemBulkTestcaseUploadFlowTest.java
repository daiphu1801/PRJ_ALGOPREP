package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
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
 * #45 and #46 over HTTP (DD F2 sections 46 and 47) with PostgreSQL, Redis and MinIO: the browser
 * side is a real HTTP PUT to the presigned URL; only the sandbox is replaced by a scripted verdict.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemBulkTestcaseUploadFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/authoring/problems";
  private static final String LEVEL = "20000000-0000-4000-8000-000000000001";
  private static final String BUCKET = "algoprep-testcases";
  private static final HttpClient HTTP = HttpClient.newHttpClient();

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
        () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
    properties.add(
        "algoprep.problem-bank.storage.public-endpoint",
        () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-testcase-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-testcase-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired StringRedisTemplate redis;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @MockitoBean SampleSolutionRunnerPort runner;

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
    reset(runner);
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
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    teacherId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(UUID.randomUUID(), "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  // ---- the whole round trip

  @Test
  void aBatchIsSignedUploadedByTheBrowserConfirmedAndInsertedTogether() throws Exception {
    var id = draft(2);
    var large = "L".repeat(9000);

    var plan = ask(id, teacher, items(pair("a", 3, 2, "SAMPLE"), pair("b", 9000, 4, "HIDDEN")));

    assertThat(plan.getStatus()).isEqualTo(200);
    var body = json(plan);
    assertThat(body.path("items")).hasSize(2);
    assertThat(body.path("expiresAt").asString()).isNotBlank();
    var a = body.path("items").get(0);
    var b = body.path("items").get(1);
    var aInput = a.path("input").path("putUrl").asString();
    assertThat(aInput).contains("problems/" + id + "/testcases/" + a.path("testcaseId").asString());
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(2);
    upload(aInput, "1 2");
    upload(a.path("expectedOutput").path("putUrl").asString(), "ok");
    upload(b.path("input").path("putUrl").asString(), large);
    upload(b.path("expectedOutput").path("putUrl").asString(), "nope");
    var uploadId = body.path("uploadId").asString();

    var done = confirm(id, teacher, uploadId);

    assertThat(done.getStatus()).isEqualTo(201);
    var added = json(done).path("added");
    assertThat(added).hasSize(2);
    assertThat(added.get(0).path("position").asInt()).isEqualTo(3);
    assertThat(added.get(1).path("position").asInt()).isEqualTo(4);
    assertThat(added.get(0).path("visibility").asString()).isEqualTo("SAMPLE");
    assertThat(added.get(0).path("origin").asString()).isEqualTo("MANUAL");
    assertThat(added.get(0).path("approved").asBoolean()).isTrue();
    assertThat(added.get(0).path("inputStorage").asString()).isEqualTo("INLINE");
    assertThat(added.get(0).path("input").asString()).isEqualTo("1 2");
    assertThat(added.get(1).path("inputStorage").asString()).isEqualTo("MINIO");
    assertThat(added.get(1).path("expectedOutputStorage").asString()).isEqualTo("INLINE");
    var largeKey = column("input_object_key", added.get(1).path("id").asString());
    assertThat(largeKey).startsWith("problems/" + id + "/testcases/");
    assertThat(exists(largeKey)).isTrue();
    assertThat(exists(aInput.substring(aInput.indexOf("problems/"), aInput.indexOf("?"))))
        .isFalse();
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(4);
  }

  @Test
  void aRepeatedConfirmAnswersTheSameAndDoesNotInsertAgain() throws Exception {
    var id = draft(0);
    var plan = json(ask(id, teacher, items(pair("a", 1, 1, "HIDDEN"))));
    var item = plan.path("items").get(0);
    upload(item.path("input").path("putUrl").asString(), "x");
    upload(item.path("expectedOutput").path("putUrl").asString(), "y");
    var uploadId = plan.path("uploadId").asString();

    var first = confirm(id, teacher, uploadId);
    var second = confirm(id, teacher, uploadId);

    assertThat(first.getStatus()).isEqualTo(201);
    assertThat(second.getStatus()).isEqualTo(201);
    assertThat(json(second).path("added").get(0).path("id").asString())
        .isEqualTo(json(first).path("added").get(0).path("id").asString());
    assertThat(json(second).path("currentTestcaseSetVersion").asInt())
        .isEqualTo(json(first).path("currentTestcaseSetVersion").asInt());
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(1);
  }

  @Test
  void aMissingObjectRefusesTheWholeBatchAndTheSessionStaysUsable() throws Exception {
    var id = draft(0);
    var plan = json(ask(id, teacher, items(pair("a", 1, 1, "HIDDEN"), pair("b", 1, 1, "HIDDEN"))));
    var a = plan.path("items").get(0);
    var b = plan.path("items").get(1);
    upload(a.path("input").path("putUrl").asString(), "x");
    upload(a.path("expectedOutput").path("putUrl").asString(), "y");
    upload(b.path("input").path("putUrl").asString(), "x");
    var uploadId = plan.path("uploadId").asString();

    var refused = confirm(id, teacher, uploadId);

    assertThat(refused.getStatus()).isEqualTo(422);
    var error = json(refused);
    assertThat(error.path("errorCode").asString()).isEqualTo("PRB-045");
    var fault = error.path("details").path("items").get(0);
    assertThat(fault.path("clientKey").asString()).isEqualTo("b");
    assertThat(fault.path("side").asString()).isEqualTo("expectedOutput");
    assertThat(fault.path("problem").asString()).isEqualTo("MISSING");
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isZero();

    upload(b.path("expectedOutput").path("putUrl").asString(), "y");
    assertThat(confirm(id, teacher, uploadId).getStatus()).isEqualTo(201);
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(2);
  }

  @Test
  void aWrongSizeAndATextThatIsNotUtf8AreRefused() throws Exception {
    var id = draft(0);
    var plan = json(ask(id, teacher, items(pair("a", 5, 1, "HIDDEN"), pair("b", 2, 1, "HIDDEN"))));
    var a = plan.path("items").get(0);
    var b = plan.path("items").get(1);
    upload(a.path("input").path("putUrl").asString(), "abc");
    upload(a.path("expectedOutput").path("putUrl").asString(), "y");
    uploadBytes(b.path("input").path("putUrl").asString(), new byte[] {(byte) 0xC3, 0x28});
    upload(b.path("expectedOutput").path("putUrl").asString(), "y");

    var refused = confirm(id, teacher, plan.path("uploadId").asString());

    assertThat(refused.getStatus()).isEqualTo(422);
    var problems = new ArrayList<String>();
    json(refused)
        .path("details")
        .path("items")
        .forEach(f -> problems.add(f.path("problem").asString()));
    assertThat(problems).containsExactly("SIZE_MISMATCH", "INVALID_TEXT");
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isZero();
  }

  @Test
  void aBatchOnAPublishedProblemBumpsTheVersionOncePublishesOneSnapshotAndOneEvent()
      throws Exception {
    var id = published();
    int before = version(id);
    int snapshotsBefore = snapshots(id);
    var plan = json(ask(id, teacher, items(pair("a", 1, 1, "SAMPLE"), pair("b", 1, 1, "HIDDEN"))));
    for (var item : plan.path("items")) {
      upload(item.path("input").path("putUrl").asString(), "x");
      upload(item.path("expectedOutput").path("putUrl").asString(), "y");
    }

    var done = confirm(id, teacher, plan.path("uploadId").asString());

    assertThat(done.getStatus()).isEqualTo(201);
    assertThat(json(done).path("currentTestcaseSetVersion").asInt()).isEqualTo(before + 1);
    assertThat(version(id)).isEqualTo(before + 1);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore + 1);
    assertThat(
            count(
                "problem.outbox_events WHERE aggregate_id = '"
                    + id
                    + "' AND event_type = 'TestcaseSetVersionBumped'"))
        .isEqualTo(1);
  }

  // ---- limits

  @Test
  void theLimitsOfARequestAreEnforced() throws Exception {
    var id = draft(0);
    var many = new ArrayList<String>();
    for (int i = 0; i < 101; i++) {
      many.add(pair("k" + i, 1, 1, "HIDDEN"));
    }

    assertThat(error(ask(id, teacher, items(many.toArray(String[]::new))))).isEqualTo("PRB-047");
    assertThat(error(ask(id, teacher, items(pair("a", 64L * 1024 * 1024 + 1, 1, "HIDDEN")))))
        .isEqualTo("PRB-047");
    long mb = 1024L * 1024;
    assertThat(
            error(
                ask(
                    id,
                    teacher,
                    items(
                        pair("a", 64 * mb, 64 * mb, "HIDDEN"),
                        pair("b", 64 * mb, 64 * mb, "HIDDEN"),
                        pair("c", 1, 1, "HIDDEN")))))
        .isEqualTo("PRB-047");
    assertThat(ask(id, teacher, items(pair("a", 64 * mb, 64 * mb, "HIDDEN"))).getStatus())
        .isEqualTo(200);
  }

  @Test
  void badItemsAreRefusedWith400() throws Exception {
    var id = draft(0);

    assertThat(ask(id, teacher, "{\"items\":[]}").getStatus()).isEqualTo(400);
    assertThat(ask(id, teacher, "{}").getStatus()).isEqualTo(400);
    assertThat(
            ask(id, teacher, items(pair("a", 1, 1, "HIDDEN"), pair("a", 1, 1, "HIDDEN")))
                .getStatus())
        .isEqualTo(400);
    assertThat(ask(id, teacher, items(pair("a", 0, 1, "HIDDEN"))).getStatus()).isEqualTo(400);
    assertThat(ask(id, teacher, items(pair("a", 1, 1, "WRONG"))).getStatus()).isEqualTo(400);
    assertThat(
            ask(id, teacher, "{\"items\":[{\"clientKey\":\"a\",\"inputSizeBytes\":\"x\"}]}")
                .getStatus())
        .isEqualTo(400);
  }

  @Test
  void aProblemHoldsAtMostFiveHundredCasesAfterTheBatch() throws Exception {
    var id = draft(0);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) SELECT gen_random_uuid(),?,'HIDDEN','INLINE','i','INLINE','o','MANUAL',true,g FROM generate_series(1,499) g",
        id);

    assertThat(error(ask(id, teacher, items(pair("a", 1, 1, "HIDDEN"), pair("b", 1, 1, "HIDDEN")))))
        .isEqualTo("PRB-046");
    assertThat(ask(id, teacher, items(pair("a", 1, 1, "HIDDEN"))).getStatus()).isEqualTo(200);
  }

  // ---- who may do it

  @Test
  void authorshipAndPermissionsDecideWhoMayUploadAndSessionsBelongToTheirOwner() throws Exception {
    var draft = draft(0);
    var published = published();
    var one = items(pair("a", 1, 1, "HIDDEN"));

    assertThat(ask(draft, rival, one).getStatus()).isEqualTo(404);
    var foreign = ask(published, rival, one);
    assertThat(foreign.getStatus()).isEqualTo(403);
    assertThat(json(foreign).path("errorCode").asString()).isEqualTo("PRB-003");
    var noPermission = ask(draft, student, one);
    assertThat(noPermission.getStatus()).isEqualTo(403);
    assertThat(json(noPermission).path("errorCode").asString()).isEqualTo("PRB-002");
    assertThat(confirm(draft, student, UUID.randomUUID().toString()).getStatus()).isEqualTo(403);

    var plan = json(ask(draft, teacher, one));
    var item = plan.path("items").get(0);
    upload(item.path("input").path("putUrl").asString(), "x");
    upload(item.path("expectedOutput").path("putUrl").asString(), "y");
    var uploadId = plan.path("uploadId").asString();
    var other = draft(0);
    var admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);

    assertThat(error(confirm(other, teacher, uploadId))).isEqualTo("PRB-044");
    assertThat(error(confirm(draft, admin, uploadId))).isEqualTo("PRB-044");
    assertThat(error(confirm(draft, teacher, UUID.randomUUID().toString()))).isEqualTo("PRB-044");
    assertThat(error(confirm(draft, teacher, "not-a-uuid"))).isEqualTo("PRB-044");
    assertThat(confirm(draft, rival, uploadId).getStatus()).isEqualTo(404);
    redis.delete("problem:upload:" + uploadId);
    assertThat(error(confirm(draft, teacher, uploadId))).isEqualTo("PRB-044");
    assertThat(count("problem.testcases WHERE problem_id = '" + draft + "'")).isZero();
  }

  // ---- helpers

  private MockHttpServletResponse ask(UUID problem, String token, String body) throws Exception {
    return send(post(BASE + "/" + problem + "/testcases/bulk-upload-url"), token, body);
  }

  private MockHttpServletResponse confirm(UUID problem, String token, String uploadId)
      throws Exception {
    return send(
        post(BASE + "/" + problem + "/testcases/bulk-upload/confirm"),
        token,
        "{\"uploadId\":\"" + uploadId + "\"}");
  }

  private String error(MockHttpServletResponse response) throws Exception {
    return json(response).path("errorCode").asString();
  }

  private static String pair(String key, long input, long expected, String visibility) {
    return "{\"clientKey\":\""
        + key
        + "\",\"inputSizeBytes\":"
        + input
        + ",\"expectedOutputSizeBytes\":"
        + expected
        + ",\"visibility\":\""
        + visibility
        + "\"}";
  }

  private static String items(String... pairs) {
    return "{\"items\":[" + String.join(",", pairs) + "]}";
  }

  private static void upload(String url, String text) throws Exception {
    uploadBytes(url, text.getBytes(StandardCharsets.UTF_8));
  }

  private static void uploadBytes(String url, byte[] bytes) throws Exception {
    var response =
        HTTP.send(
            HttpRequest.newBuilder(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
  }

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String token, String body) throws Exception {
    request.header("Authorization", "Bearer " + token);
    if (!body.isEmpty()) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Upload actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** An UNPUBLISHED problem with {@code cases} approved HIDDEN cases. */
  private UUID draft(int cases) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Upload fixture','statement',?,?::uuid,'UNPUBLISHED',1000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL);
    for (int i = 1; i <= cases; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
          UUID.randomUUID(),
          id,
          i <= 2 ? "SAMPLE" : "HIDDEN",
          "in" + i,
          "out" + i,
          i);
    }
    return id;
  }

  /** A problem published through the real endpoint, so it has its first snapshot. */
  private UUID published() throws Exception {
    var id = draft(8);
    for (int i = 1; i <= 2; i++) {
      jdbc.update(
          "INSERT INTO problem.worked_examples(id,problem_id,position,input,output) VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          i,
          "i" + i,
          "o" + i);
    }
    jdbc.update(
        "INSERT INTO problem.problem_reference_solutions(id,problem_id,language,source_code) VALUES (?,?,'PYTHON','def solve(): pass')",
        UUID.randomUUID(),
        id);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'in','out','solve','{\"kind\":\"INT\"}'::jsonb,'[]'::jsonb)",
        UUID.randomUUID(),
        id);
    var response =
        send(
            put(BASE + "/" + id + "/status"),
            teacher,
            "{\"status\":\"PUBLISHED\",\"expectedUpdatedAt\":\"" + stamp(id) + "\"}");
    assertThat(response.getStatus()).isEqualTo(200);
    return id;
  }

  private int version(UUID id) {
    return jdbc.queryForObject(
        "SELECT current_testcase_set_version FROM problem.problems WHERE id=?", Integer.class, id);
  }

  private int snapshots(UUID id) {
    return count("problem.testcase_set_snapshots WHERE problem_id = '" + id + "'");
  }

  private String column(String name, String testcase) {
    return jdbc.queryForObject(
        "SELECT " + name + " FROM problem.testcases WHERE id=?::uuid", String.class, testcase);
  }

  private boolean exists(String key) {
    try {
      minio.statObject(StatObjectArgs.builder().bucket(BUCKET).object(key).build());
      return true;
    } catch (Exception missing) {
      return false;
    }
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
  }

  private String stamp(UUID id) {
    return jdbc.queryForObject(
            "SELECT updated_at FROM problem.problems WHERE id=?", Timestamp.class, id)
        .toInstant()
        .toString();
  }
}
