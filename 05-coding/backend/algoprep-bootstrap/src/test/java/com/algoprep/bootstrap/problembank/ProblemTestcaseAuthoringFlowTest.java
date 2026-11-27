package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
 * #41 to #44 over HTTP (DD F2 sections 42 to 45) with PostgreSQL and MinIO, the real snapshot
 * kernel and the real publish path; only the sandbox is replaced by a scripted verdict.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ProblemTestcaseAuthoringFlowTest {
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

  @MockitoBean SampleSolutionRunnerPort runner;

  // Passes through to the real MinIO store; only the broken-store test makes `put` fail.
  @MockitoSpyBean TestcaseObjectStorePort objectStore;

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

  // ---- create (#41)

  @Test
  void aSmallHiddenCaseOnADraftIsInlineApprovedAndLastWithoutANewVersion() throws Exception {
    var id = draft(2, 1);
    int version = version(id);

    var response = send(post(cases(id)), teacher, "{\"input\":\"1 2\",\"expectedOutput\":\"3\"}");

    assertThat(response.getStatus()).isEqualTo(201);
    var json = json(response);
    var created = json.path("testcase");
    assertThat(created.path("visibility").asString()).isEqualTo("HIDDEN");
    assertThat(created.path("origin").asString()).isEqualTo("MANUAL");
    assertThat(created.path("approved").asBoolean()).isTrue();
    assertThat(created.path("position").asInt()).isEqualTo(3);
    assertThat(created.path("inputStorage").asString()).isEqualTo("INLINE");
    assertThat(json.path("currentTestcaseSetVersion").asInt()).isEqualTo(version);
    assertThat(json.path("problemUpdatedAt").asString()).isNotBlank();
    assertThat(snapshots(id)).isZero();
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(3);
  }

  @Test
  void eachSideIsStoredByItsOwnSizeAtTheEightKilobyteBoundary() throws Exception {
    var id = draft(0, 0);
    var edge = "a".repeat(8192);
    var over = "b".repeat(8193);

    var first =
        json(
            send(
                post(cases(id)),
                teacher,
                "{\"input\":\"" + edge + "\",\"expectedOutput\":\"" + over + "\"}"));

    var created = first.path("testcase");
    assertThat(created.path("inputStorage").asString()).isEqualTo("INLINE");
    assertThat(created.path("expectedOutputStorage").asString()).isEqualTo("MINIO");
    assertThat(created.path("expectedOutputSizeBytes").asLong()).isEqualTo(8193);
    var key =
        jdbc.queryForObject(
            "SELECT expected_output_object_key FROM problem.testcases WHERE id = ?::uuid",
            String.class,
            created.path("id").asString());
    assertThat(key).startsWith("problems/" + id + "/testcases/" + created.path("id").asString());
    assertThat(exists(key)).isTrue();
  }

  @Test
  void aCreateOnAPublishedProblemBumpsTheVersionOnceAndPublishesASnapshotAndAnEvent()
      throws Exception {
    var id = published(8, 2);
    int before = version(id);
    int snapshotsBefore = snapshots(id);

    var response =
        send(
            post(cases(id)),
            teacher,
            "{\"visibility\":\"SAMPLE\",\"input\":\"x\",\"expectedOutput\":\"y\"}");

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(json(response).path("currentTestcaseSetVersion").asInt()).isEqualTo(before + 1);
    assertThat(version(id)).isEqualTo(before + 1);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore + 1);
    assertThat(
            count(
                "problem.outbox_events WHERE aggregate_id = '"
                    + id
                    + "' AND event_type = 'TestcaseSetVersionBumped'"))
        .isEqualTo(1);
  }

  @Test
  void badInputIsRejectedAndNothingIsWritten() throws Exception {
    var id = draft(0, 0);
    var path = cases(id);

    assertThat(send(post(path), teacher, "{\"input\":\"\",\"expectedOutput\":\"1\"}").getStatus())
        .isEqualTo(400);
    assertThat(send(post(path), teacher, "{\"input\":\"1\"}").getStatus()).isEqualTo(400);
    assertThat(
            send(
                    post(path),
                    teacher,
                    "{\"visibility\":\"X\",\"input\":\"1\",\"expectedOutput\":\"1\"}")
                .getStatus())
        .isEqualTo(400);
    assertThat(
            send(
                    post(path),
                    teacher,
                    "{\"category\":\"X\",\"input\":\"1\",\"expectedOutput\":\"1\"}")
                .getStatus())
        .isEqualTo(400);
    var big = "z".repeat(1_048_577);
    var tooBig = send(post(path), teacher, "{\"input\":\"" + big + "\",\"expectedOutput\":\"1\"}");
    assertThat(tooBig.getStatus()).isEqualTo(422);
    assertThat(json(tooBig).path("errorCode").asString()).isEqualTo("PRB-048");
    var atLimit = "z".repeat(1_048_576);
    assertThat(
            send(post(path), teacher, "{\"input\":\"" + atLimit + "\",\"expectedOutput\":\"1\"}")
                .getStatus())
        .isEqualTo(201);
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(1);
  }

  @Test
  void aProblemHoldsAtMostFiveHundredCases() throws Exception {
    var id = draft(0, 0);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) SELECT gen_random_uuid(),?,'HIDDEN','INLINE','i','INLINE','o','MANUAL',true,g FROM generate_series(1,500) g",
        id);

    var response = send(post(cases(id)), teacher, "{\"input\":\"1\",\"expectedOutput\":\"1\"}");

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(json(response).path("errorCode").asString()).isEqualTo("PRB-046");
    assertThat(count("problem.testcases WHERE problem_id = '" + id + "'")).isEqualTo(500);
  }

  @Test
  void permissionsAndAuthorshipDecideWhoMayChangeCases() throws Exception {
    var draft = draft(1, 1);
    var published = published(8, 2);
    var body = "{\"input\":\"1\",\"expectedOutput\":\"1\"}";
    var own = firstCase(draft);

    assertThat(send(post(cases(draft)), rival, body).getStatus()).isEqualTo(404);
    var foreign = send(post(cases(published)), rival, body);
    assertThat(foreign.getStatus()).isEqualTo(403);
    assertThat(json(foreign).path("errorCode").asString()).isEqualTo("PRB-003");
    var noPermission = send(post(cases(draft)), student, body);
    assertThat(noPermission.getStatus()).isEqualTo(403);
    assertThat(json(noPermission).path("errorCode").asString()).isEqualTo("PRB-002");
    assertThat(
            send(patch(cases(draft) + "/" + own), rival, "{\"category\":\"TYPICAL\"}").getStatus())
        .isEqualTo(404);
    assertThat(send(delete(cases(published) + "/" + firstCase(published)), rival, "").getStatus())
        .isEqualTo(403);
    assertThat(send(put(cases(published) + "/order"), rival, "{\"orderedIds\":[]}").getStatus())
        .isEqualTo(403);
    assertThat(count("problem.testcases WHERE problem_id = '" + published + "'")).isEqualTo(8);
  }

  // ---- update (#42)

  @Test
  void aCategoryChangeNeverBumpsButAContentChangeDoes() throws Exception {
    var id = published(8, 2);
    var target = firstCase(id);
    int before = version(id);

    var category = send(patch(cases(id) + "/" + target), teacher, "{\"category\":\"EXTREME\"}");
    assertThat(category.getStatus()).isEqualTo(200);
    assertThat(json(category).path("testcase").path("category").asString()).isEqualTo("EXTREME");
    assertThat(version(id)).isEqualTo(before);

    var content =
        send(patch(cases(id) + "/" + target), teacher, "{\"expectedOutput\":\"changed\"}");
    assertThat(content.getStatus()).isEqualTo(200);
    assertThat(version(id)).isEqualTo(before + 1);
    var cleared = send(patch(cases(id) + "/" + target), teacher, "{\"category\":null}");
    assertThat(json(cleared).path("testcase").path("category").isNull()).isTrue();
    assertThat(version(id)).isEqualTo(before + 1);
  }

  @Test
  void approvingAnAiDraftKeepsItsOriginAndBumpsAndUnapprovingIsRefused() throws Exception {
    var id = published(8, 2);
    int before = version(id);
    var draft = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','ai','INLINE','out','AI',false,99)",
        draft,
        id);

    var edited = send(patch(cases(id) + "/" + draft), teacher, "{\"input\":\"ai2\"}");
    assertThat(edited.getStatus()).isEqualTo(200);
    assertThat(version(id)).isEqualTo(before);

    var approved = send(patch(cases(id) + "/" + draft), teacher, "{\"approved\":true}");
    var json = json(approved).path("testcase");
    assertThat(json.path("approved").asBoolean()).isTrue();
    assertThat(json.path("origin").asString()).isEqualTo("AI");
    assertThat(version(id)).isEqualTo(before + 1);

    var back = send(patch(cases(id) + "/" + draft), teacher, "{\"approved\":false}");
    assertThat(back.getStatus()).isEqualTo(400);
    assertThat(json(back).path("errorCode").asString()).isEqualTo("PRB-001");
  }

  @Test
  void aSampleCannotBecomeHiddenBelowTheSampleMinimumOfAPublishedProblem() throws Exception {
    var tight = published(8, 2);
    var roomy = published(9, 3);
    int tightBefore = version(tight);
    int roomyBefore = version(roomy);

    var refused =
        send(patch(cases(tight) + "/" + firstCase(tight)), teacher, "{\"visibility\":\"HIDDEN\"}");
    assertThat(refused.getStatus()).isEqualTo(422);
    assertThat(json(refused).path("errorCode").asString()).isEqualTo("PRB-054");
    assertThat(version(tight)).isEqualTo(tightBefore);
    assertThat(
            count("problem.testcases WHERE problem_id = '" + tight + "' AND visibility = 'SAMPLE'"))
        .isEqualTo(2);

    var allowed =
        send(patch(cases(roomy) + "/" + firstCase(roomy)), teacher, "{\"visibility\":\"HIDDEN\"}");
    assertThat(allowed.getStatus()).isEqualTo(200);
    assertThat(version(roomy)).isEqualTo(roomyBefore + 1);
  }

  @Test
  void aSideMovesBetweenInlineAndMinioAndTheOldObjectIsRemoved() throws Exception {
    var id = draft(1, 1);
    var target = firstCase(id);

    send(patch(cases(id) + "/" + target), teacher, "{\"input\":\"" + "q".repeat(9000) + "\"}");
    var key = column("input_object_key", target);
    assertThat(column("input_storage", target)).isEqualTo("MINIO");
    assertThat(exists(key)).isTrue();

    send(patch(cases(id) + "/" + target), teacher, "{\"input\":\"small\"}");
    assertThat(column("input_storage", target)).isEqualTo("INLINE");
    assertThat(exists(key)).isFalse();
    assertThat(
            send(patch(cases(id) + "/" + UUID.randomUUID()), teacher, "{\"input\":\"x\"}")
                .getStatus())
        .isEqualTo(404);
    var other = draft(1, 1);
    var foreignCase = send(patch(cases(id) + "/" + firstCase(other)), teacher, "{\"input\":\"x\"}");
    assertThat(foreignCase.getStatus()).isEqualTo(404);
    assertThat(json(foreignCase).path("errorCode").asString()).isEqualTo("PRB-011");
  }

  // ---- delete (#43)

  @Test
  void deletingAnApprovedCaseOfAPublishedProblemBumpsAndAnAiDraftDoesNot() throws Exception {
    var id = published(9, 2);
    int before = version(id);
    var draft = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,'HIDDEN','INLINE','ai','INLINE','out','AI',false,99)",
        draft,
        id);

    var gone = send(delete(cases(id) + "/" + draft), teacher, "");
    assertThat(gone.getStatus()).isEqualTo(200);
    assertThat(version(id)).isEqualTo(before);

    var hidden = hiddenCase(id);
    var removed = send(delete(cases(id) + "/" + hidden), teacher, "");
    assertThat(removed.getStatus()).isEqualTo(200);
    assertThat(json(removed).path("currentTestcaseSetVersion").asInt()).isEqualTo(before + 1);
    assertThat(count("problem.testcases WHERE id = '" + hidden + "'")).isZero();
    var twice = send(delete(cases(id) + "/" + hidden), teacher, "");
    assertThat(twice.getStatus()).isEqualTo(404);
    assertThat(json(twice).path("errorCode").asString()).isEqualTo("PRB-011");
  }

  @Test
  void aPublishedProblemKeepsItsMinimumsWhenACaseIsDeleted() throws Exception {
    var minimum = published(8, 2);
    int before = version(minimum);

    var refused = send(delete(cases(minimum) + "/" + hiddenCase(minimum)), teacher, "");

    assertThat(refused.getStatus()).isEqualTo(422);
    assertThat(json(refused).path("errorCode").asString()).isEqualTo("PRB-054");
    assertThat(count("problem.testcases WHERE problem_id = '" + minimum + "'")).isEqualTo(8);
    assertThat(version(minimum)).isEqualTo(before);
    var draft = draft(1, 1);
    assertThat(send(delete(cases(draft) + "/" + firstCase(draft)), teacher, "").getStatus())
        .isEqualTo(200);
  }

  @Test
  void deletingAMinioCaseRemovesItsObjects() throws Exception {
    var id = draft(0, 0);
    var big = "m".repeat(9000);
    var created =
        json(
            send(
                post(cases(id)),
                teacher,
                "{\"input\":\"" + big + "\",\"expectedOutput\":\"" + big + "\"}"));
    var caseId = created.path("testcase").path("id").asString();
    var inKey = column("input_object_key", UUID.fromString(caseId));
    var outKey = column("expected_output_object_key", UUID.fromString(caseId));
    assertThat(exists(inKey)).isTrue();

    assertThat(send(delete(cases(id) + "/" + caseId), teacher, "").getStatus()).isEqualTo(200);

    assertThat(exists(inKey)).isFalse();
    assertThat(exists(outKey)).isFalse();
  }

  @Test
  void aFailedObjectCleanupAfterADeleteStillSucceedsAndLeavesOnlyAWarning(CapturedOutput output)
      throws Exception {
    var id = draft(0, 0);
    var big = "m".repeat(9000);
    var created =
        json(
            send(
                post(cases(id)),
                teacher,
                "{\"input\":\"" + big + "\",\"expectedOutput\":\"" + big + "\"}"));
    var caseId = UUID.fromString(created.path("testcase").path("id").asString());
    var inKey = column("input_object_key", caseId);
    var outKey = column("expected_output_object_key", caseId);
    doThrow(new IllegalStateException("minio is down")).when(objectStore).delete(anyString());

    var response = send(delete(cases(id) + "/" + caseId), teacher, "");

    // The row is already committed, so the caller sees success; the two objects are left behind.
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(count("problem.testcases WHERE id = '" + caseId + "'")).isZero();
    assertThat(exists(inKey)).isTrue();
    assertThat(exists(outKey)).isTrue();
    assertThat(output.getAll())
        .contains("Testcase object cleanup failed for key " + inKey)
        .contains("Testcase object cleanup failed for key " + outKey);
  }

  // ---- reorder (#44)

  @Test
  void aNewOrderOnAPublishedProblemRenumbersAndPublishesOneNewVersion() throws Exception {
    var id = published(8, 2);
    int before = version(id);
    int snapshotsBefore = snapshots(id);
    var ids = new ArrayList<>(orderedIds(id));
    var last = ids.remove(ids.size() - 1);
    ids.add(0, last);

    var response = send(put(cases(id) + "/order"), teacher, order(ids));

    assertThat(response.getStatus()).isEqualTo(200);
    var json = json(response);
    assertThat(json.path("items").get(0).path("id").asString()).isEqualTo(last.toString());
    assertThat(json.path("items").get(0).path("position").asInt()).isEqualTo(1);
    assertThat(orderedIds(id)).isEqualTo(ids);
    assertThat(version(id)).isEqualTo(before + 1);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore + 1);

    var same = send(put(cases(id) + "/order"), teacher, order(ids));
    assertThat(same.getStatus()).isEqualTo(200);
    assertThat(version(id)).isEqualTo(before + 1);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore + 1);
    assertThat(
            count(
                "problem.outbox_events WHERE aggregate_id = '"
                    + id
                    + "' AND event_type = 'TestcaseSetVersionBumped'"))
        .isEqualTo(1);
  }

  @Test
  void aDraftKeepsTheOrderWithoutAVersionAndAWrongSetIsRefused() throws Exception {
    var id = draft(3, 1);
    int before = version(id);
    var ids = new ArrayList<>(orderedIds(id));
    java.util.Collections.reverse(ids);

    assertThat(send(put(cases(id) + "/order"), teacher, order(ids)).getStatus()).isEqualTo(200);
    assertThat(orderedIds(id)).isEqualTo(ids);
    assertThat(version(id)).isEqualTo(before);
    assertThat(snapshots(id)).isZero();

    var missing = new ArrayList<>(ids);
    missing.remove(0);
    var refused = send(put(cases(id) + "/order"), teacher, order(missing));
    assertThat(refused.getStatus()).isEqualTo(422);
    assertThat(json(refused).path("errorCode").asString()).isEqualTo("PRB-049");
    var duplicated = new ArrayList<>(ids);
    duplicated.set(0, duplicated.get(1));
    assertThat(send(put(cases(id) + "/order"), teacher, order(duplicated)).getStatus())
        .isEqualTo(422);
    assertThat(orderedIds(id)).isEqualTo(ids);

    var empty = draft(0, 0);
    assertThat(send(put(cases(empty) + "/order"), teacher, "{\"orderedIds\":[]}").getStatus())
        .isEqualTo(200);
  }

  @Test
  void aBrokenObjectStoreRollsBackTheReorderOfAPublishedProblemAndARetryWorks() throws Exception {
    var id = published(8, 2);
    int before = version(id);
    int snapshotsBefore = snapshots(id);
    int eventsBefore = count("problem.outbox_events WHERE aggregate_id = '" + id + "'");
    var original = orderedIds(id);
    var reversed = new ArrayList<>(original);
    java.util.Collections.reverse(reversed);
    clearInvocations(objectStore);
    doThrow(new IllegalStateException("minio is down"))
        .when(objectStore)
        .put(anyString(), any(byte[].class));

    var failed = send(put(cases(id) + "/order"), teacher, order(reversed));

    // The snapshot write was attempted, so the failure came from the store and not from validation.
    verify(objectStore).put(anyString(), any(byte[].class));
    assertThat(failed.getStatus()).isEqualTo(503);
    assertThat(json(failed).path("errorCode").asString()).isEqualTo("PRB-052");
    assertThat(failed.getContentAsString()).doesNotContain("minio is down");
    assertThat(orderedIds(id)).isEqualTo(original);
    assertThat(version(id)).isEqualTo(before);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore);
    assertThat(count("problem.outbox_events WHERE aggregate_id = '" + id + "'"))
        .isEqualTo(eventsBefore);

    doCallRealMethod().when(objectStore).put(anyString(), any(byte[].class));
    var retried = send(put(cases(id) + "/order"), teacher, order(reversed));

    assertThat(retried.getStatus()).isEqualTo(200);
    assertThat(orderedIds(id)).isEqualTo(reversed);
    assertThat(version(id)).isEqualTo(before + 1);
    assertThat(snapshots(id)).isEqualTo(snapshotsBefore + 1);
  }

  // ---- helpers

  private static String cases(UUID problem) {
    return BASE + "/" + problem + "/testcases";
  }

  private static String order(List<UUID> ids) {
    return "{\"orderedIds\":[\""
        + String.join("\",\"", ids.stream().map(UUID::toString).toList())
        + "\"]}";
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
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Testcase actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** An UNPUBLISHED problem with {@code cases} approved cases, the first {@code samples} SAMPLE. */
  private UUID draft(int cases, int samples) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Testcase fixture','statement',?,?::uuid,'UNPUBLISHED',1000,256,64)",
        id,
        id.toString(),
        teacherId,
        LEVEL);
    for (int i = 1; i <= cases; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',true,?)",
          UUID.randomUUID(),
          id,
          i <= samples ? "SAMPLE" : "HIDDEN",
          "in" + i,
          "out" + i,
          i);
    }
    return id;
  }

  /** A problem published through the real endpoint, so it has its first snapshot. */
  private UUID published(int cases, int samples) throws Exception {
    var id = draft(cases, samples);
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

  private UUID firstCase(UUID problem) {
    return orderedIds(problem).get(0);
  }

  private UUID hiddenCase(UUID problem) {
    return jdbc.queryForObject(
        "SELECT id FROM problem.testcases WHERE problem_id=? AND visibility='HIDDEN' AND approved ORDER BY position LIMIT 1",
        UUID.class,
        problem);
  }

  private List<UUID> orderedIds(UUID problem) {
    return jdbc.queryForList(
        "SELECT id FROM problem.testcases WHERE problem_id=? AND position < 99 ORDER BY position, id",
        UUID.class,
        problem);
  }

  private int version(UUID id) {
    return jdbc.queryForObject(
        "SELECT current_testcase_set_version FROM problem.problems WHERE id=?", Integer.class, id);
  }

  private int snapshots(UUID id) {
    return count("problem.testcase_set_snapshots WHERE problem_id = '" + id + "'");
  }

  private String column(String name, UUID testcase) {
    return jdbc.queryForObject(
        "SELECT " + name + " FROM problem.testcases WHERE id=?", String.class, testcase);
  }

  private String column(String name, String testcase) {
    return column(name, UUID.fromString(testcase));
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
