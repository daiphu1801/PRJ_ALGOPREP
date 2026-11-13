package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.in.GetOwnedSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.GetSubmissionProgressUseCase;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real JWT/HTTP dispatch, PostgreSQL, historical F1 scope, F2 metadata and worker writes. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=8"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(SubmissionReadFlowTest.Recording.class)
class SubmissionReadFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired JdbcTemplate jdbc;
  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort access;
  @Autowired GetOwnedSubmissionDetailUseCase details;
  @Autowired GetSubmissionProgressUseCase progress;
  @Autowired BeginSubmissionCompilationUseCase compile;
  @Autowired BeginSubmissionJudgingUseCase judging;
  @Autowired IssueSubmissionRunTokenUseCase tokens;
  @Autowired RecordSubmissionTestcaseUseCase results;
  @Autowired FinalizeSubmissionUseCase finalizer;
  @MockitoSpyBean SubmissionReadRepository repository;
  @MockitoSpyBean ProblemSummaryReadPort metadata;
  @MockitoSpyBean ClassScopeReadPort scope;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID student;
  private UUID stranger;
  private UUID instructor;
  private UUID problem;
  private Instant time;

  @TestConfiguration
  static class Recording {
    static final List<com.algoprep.judge.application.dto.SubmissionProgress> EVENTS =
        new ArrayList<>();

    @Bean
    @Primary
    SubmissionProgressPort readTestProgress() {
      return (owner, event) ->
          TransactionSynchronizationManager.registerSynchronization(
              new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                  EVENTS.add(event);
                }
              });
    }
  }

  @BeforeEach
  void fixture() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM judge.outbox_events");
    jdbc.update("DELETE FROM judge.submission_testcase_results");
    jdbc.update("DELETE FROM judge.callback_logs");
    jdbc.update("DELETE FROM judge.judge_run_tokens");
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update("UPDATE judge.sandbox_settings SET return_stderr_to_student=true");
    student = user("STUDENT");
    stranger = user("STUDENT");
    instructor = user("INSTRUCTOR");
    problem = problem("Two sums");
    time = jdbc.queryForObject("SELECT now()", java.time.OffsetDateTime.class).toInstant();
    Recording.EVENTS.clear();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Learner',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private UUID problem(String title) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,?,?,'20000000-0000-4000-8000-000000000001','PUBLISHED',1000,256,64)",
        id,
        id.toString(),
        title,
        instructor);
    return id;
  }

  private String token(UUID id, String role) {
    return access.issue(id, BaseCategory.valueOf(role), role, UUID.randomUUID()).value();
  }

  private JsonNode request(String path, UUID actor, String role, int status) throws Exception {
    var response =
        mvc.perform(
                get("/api/v1/judge" + path).header("Authorization", "Bearer " + token(actor, role)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode own(UUID id) throws Exception {
    return request("/submissions/" + id, student, "STUDENT", 200);
  }

  private UUID submission(String status) {
    return submission(student, problem, status, time);
  }

  private UUID submission(UUID owner, UUID p, String status, Instant submitted) {
    UUID id = UUID.randomUUID();
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    boolean scored = terminal && !List.of("COMPILE_ERROR", "SYSTEM_ERROR").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,
        status,failure_reason,compile_error_message,passed_testcase_count,total_testcase_count,score_ratio,
        runtime_ms,memory_kb,testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source','EDITOR',?,?,?,1,?,?,100,256,1,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        id,
        owner,
        p,
        status,
        status.equals("SYSTEM_ERROR") ? "WORKER_TIMEOUT" : null,
        status.equals("COMPILE_ERROR") ? "safe compiler error" : null,
        status.equals("PENDING") || status.equals("COMPILING") || status.equals("COMPILE_ERROR")
            ? null
            : 2,
        scored ? new java.math.BigDecimal("0.500") : null,
        submitted.toString(),
        terminal ? submitted.toString() : null);
    return id;
  }

  private UUID testcase(UUID sub, int order, boolean sample, String verdict) {
    UUID test = UUID.randomUUID();
    UUID run = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,
        expected_output_inline,approved,position) VALUES(?, ?, ?, 'INLINE','secret-input','INLINE','secret-expected',true,?)
        """,
        test,
        problem,
        sample ? "SAMPLE" : "HIDDEN",
        order);
    jdbc.update(
        "INSERT INTO judge.judge_run_tokens(id,submission_id,testcase_id,token) VALUES(?,?,?,?)",
        run,
        sub,
        test,
        UUID.randomUUID());
    jdbc.update(
        """
        INSERT INTO judge.submission_testcase_results(id,submission_id,testcase_id,is_sample,testcase_order,verdict,
        runtime_ms,memory_kb,actual_output_snippet,stderr_snippet,judge_run_token_id) VALUES(?,?,?,?,?,?,100,256,?,?,?)
        """,
        UUID.randomUUID(),
        sub,
        test,
        sample,
        order,
        verdict,
        sample ? "sample-output" : null,
        sample && !verdict.equals("AC") ? "sample-stderr" : null,
        run);
    return test;
  }

  private UUID classroom(UUID teacher, Instant joined, Instant left) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES(?,?,'Class')", id, teacher);
    jdbc.update(
        "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at,left_at) VALUES(?,?,?,CAST(? AS timestamptz),CAST(? AS timestamptz))",
        UUID.randomUUID(),
        id,
        student,
        joined.toString(),
        left == null ? null : left.toString());
    return id;
  }

  private void assignment(UUID classId, Instant assigned, Instant removed) {
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at,removed_at) VALUES(?,?,?,?,CAST(? AS timestamptz),CAST(? AS timestamptz))",
        UUID.randomUUID(),
        problem,
        classId,
        instructor,
        assigned.toString(),
        removed == null ? null : removed.toString());
  }

  @Test
  void acceptedBeatsUseSameProblemLanguageExcludeSelfAndRoundHalfUp() throws Exception {
    UUID current = submission("ACCEPTED");
    for (int i = 0; i < 39; i++) {
      UUID other = submission(stranger, problem, "ACCEPTED", time);
      jdbc.update("UPDATE judge.submissions SET runtime_ms=? WHERE id=?", i < 19 ? 100 : 90, other);
    }
    UUID cpp = submission("ACCEPTED");
    jdbc.update("UPDATE judge.submissions SET language='CPP' WHERE id=?", cpp);
    submission(student, problem("Other"), "ACCEPTED", time);
    submission("WRONG_ANSWER");
    var result = own(current);
    assertThat(result.path("beatsSampleSize").asLong()).isEqualTo(40);
    assertThat(result.path("beatsPercent").asInt()).isEqualTo(48);
    assertThat(result.path("sourceCode").asString()).isEqualTo("private-source");
    assertThat(own(cpp).path("beatsPercent").isNull()).isTrue();
  }

  @Test
  void beatsStartAtTenAndNeverAppearForOtherVerdicts() throws Exception {
    UUID current = submission("ACCEPTED");
    for (int i = 0; i < 8; i++) {
      submission("ACCEPTED");
    }
    assertThat(own(current).path("beatsSampleSize").asInt()).isEqualTo(9);
    assertThat(own(current).path("beatsPercent").isNull()).isTrue();
    submission("ACCEPTED");
    assertThat(own(current).path("beatsPercent").asInt()).isEqualTo(90);
    assertThat(own(submission("WRONG_ANSWER")).path("beatsSampleSize").isNull()).isTrue();
  }

  @Test
  void ownershipMalformedIdsAndNoTokenCannotRevealSubmissionExistence() throws Exception {
    UUID id = submission("WRONG_ANSWER");
    for (String suffix : List.of("", "/progress")) {
      var foreign = request("/submissions/" + id + suffix, stranger, "STUDENT", 404);
      var missing = request("/submissions/" + UUID.randomUUID() + suffix, stranger, "STUDENT", 404);
      var invalid = request("/submissions/bad" + suffix, stranger, "STUDENT", 404);
      sameNotFound(foreign, missing);
      sameNotFound(foreign, invalid);
      assertThat(foreign.path("errorCode").asString()).isEqualTo("JDG-110");
    }
    assertThat(
            mvc.perform(get("/api/v1/judge/submissions/" + id))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    request("/submissions/" + id, instructor, "INSTRUCTOR", 403);
  }

  @Test
  void doubleVisibilityGuardsAlsoApplyAfterCurrentVisibilityChanges() throws Exception {
    UUID id = submission("WRONG_ANSWER");
    UUID sample = testcase(id, 1, true, "WA");
    UUID hidden = testcase(id, 2, false, "WA");
    var lines = own(id).path("testcaseDetail").path("items");
    assertThat(lines.get(0).path("actualOutput").asString()).isEqualTo("sample-output");
    assertHidden(lines.get(1));
    jdbc.update("UPDATE problem.testcases SET visibility='HIDDEN' WHERE id=?", sample);
    jdbc.update("UPDATE problem.testcases SET visibility='SAMPLE' WHERE id=?", hidden);
    var changed = own(id);
    assertHidden(changed.path("testcaseDetail").path("items").get(0));
    assertHidden(changed.path("testcaseDetail").path("items").get(1));
    assertThat(json.writeValueAsString(changed)).doesNotContain("secret-input", "secret-expected");
  }

  private static void assertHidden(JsonNode line) {
    assertThat(line.path("visibility").asString()).isEqualTo("HIDDEN");
    assertThat(line.path("testcaseId").isNull()).isTrue();
    assertThat(line.path("actualOutput").isNull()).isTrue();
    assertThat(line.path("stderrSnippet").isNull()).isTrue();
  }

  @Test
  void stderrRequiresSampleStandardIoFlagAndNonAc() throws Exception {
    UUID id = submission("WRONG_ANSWER");
    testcase(id, 1, true, "WA");
    testcase(id, 2, true, "AC");
    var lines = own(id).path("testcaseDetail").path("items");
    assertThat(lines.get(0).path("stderrSnippet").asString()).isEqualTo("sample-stderr");
    assertThat(lines.get(1).path("stderrSnippet").isNull()).isTrue();
    jdbc.update("UPDATE judge.sandbox_settings SET return_stderr_to_student=false");
    assertThat(own(id).path("testcaseDetail").path("items").get(0).path("stderrSnippet").isNull())
        .isTrue();
    jdbc.update("UPDATE judge.sandbox_settings SET return_stderr_to_student=true");
    jdbc.update("UPDATE judge.submissions SET submission_model='FUNCTION_WRAPPER' WHERE id=?", id);
    assertThat(own(id).path("testcaseDetail").path("items").get(0).path("stderrSnippet").isNull())
        .isTrue();
  }

  @Test
  void realF2SqlFailureFailsClosedWithoutPoisoningTheResultTransaction() throws Exception {
    UUID id = submission("WRONG_ANSWER");
    testcase(id, 1, true, "WA");
    jdbc.execute("ALTER TABLE problem.testcases RENAME TO testcase_read_fault");
    try {
      var result = own(id);
      assertHidden(result.path("testcaseDetail").path("items").get(0));
      assertThat(result.path("sourceCode").asString()).isEqualTo("private-source");
    } finally {
      jdbc.execute("ALTER TABLE problem.testcase_read_fault RENAME TO testcases");
    }
  }

  @Test
  void summaryFailureKeepsResultsAndWithdrawnProblemsKeepNames() throws Exception {
    UUID id = submission("WRONG_ANSWER");
    testcase(id, 1, true, "WA");
    jdbc.update(
        "UPDATE problem.problems SET status='UNPUBLISHED',deleted=true WHERE id=?", problem);
    assertThat(own(id).path("problem").path("withdrawn").asBoolean()).isTrue();
    doThrow(new IllegalStateException("private SQL")).when(metadata).summaries(any());
    var result = own(id);
    assertThat(result.path("problem").isNull()).isTrue();
    assertThat(result.path("testcaseDetail").path("items")).hasSize(1);
  }

  @Test
  void ninetyDayBoundaryIsAvailableThenExpiresWithoutWaitingForCleanup() {
    UUID id = submission("WRONG_ANSWER");
    testcase(id, 1, true, "WA");
    // details.get runs outside any caller transaction (NOT_SUPPORTED, so it never holds a
    // connection
    // while F2 opens a second one). now() is then not frozen between the update and the read, so
    // the
    // exact microsecond boundary cannot be asserted here; check a 5 second margin on both sides.
    jdbc.update(
        "UPDATE judge.submissions SET submitted_at=now()-interval '90 days'+interval '5 seconds' WHERE id=?",
        id);
    assertThat(details.get(student, id).testcaseDetail().available()).isTrue();
    jdbc.update(
        "UPDATE judge.submissions SET submitted_at=now()-interval '90 days'-interval '5 seconds' WHERE id=?",
        id);
    var expired = details.get(student, id);
    assertThat(expired.testcaseDetail().unavailableReason()).isEqualTo("RETENTION_EXPIRED");
    assertThat(expired.scoreRatio()).isEqualByComparingTo("0.5");
    assertThat(expired.sourceCode()).isEqualTo("private-source");
    assertThat(progress.getProgress(student, id).testcases()).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                Integer.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void errorOutcomesKeepPartialTestcasesButNeverExposeScores() throws Exception {
    UUID compileError = submission("COMPILE_ERROR");
    assertThat(own(compileError).path("compileErrorMessage").asString())
        .isEqualTo("safe compiler error");
    assertThat(own(compileError).path("testcaseDetail").path("unavailableReason").asString())
        .isEqualTo("NOT_APPLICABLE");
    UUID systemError = submission("SYSTEM_ERROR");
    assertThat(own(systemError).path("testcaseDetail").path("available").asBoolean()).isFalse();
    testcase(systemError, 1, true, "WA");
    var partial = own(systemError);
    assertThat(partial.path("testcaseDetail").path("items")).hasSize(1);
    assertThat(partial.path("scoreRatio").isNull()).isTrue();
    assertThat(partial.path("passedCount").isNull()).isTrue();
    assertThat(own(submission("PENDING")).path("testcaseDetail").path("available").asBoolean())
        .isTrue();
  }

  @Test
  void recentHistoryIsOwnedLimitedAndIncludesWithdrawnProblems() throws Exception {
    for (int i = 0; i < 12; i++) {
      submission(student, problem, "WRONG_ANSWER", time.plusSeconds(i));
    }
    submission(stranger, problem, "ACCEPTED", time.plusSeconds(99));
    UUID pending = submission(student, problem, "PENDING", time.plusSeconds(100));
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", problem);
    var result =
        request("/problems/" + problem + "/submissions/mine", student, "STUDENT", 200)
            .path("items");
    assertThat(result).hasSize(10);
    assertThat(result.get(0).path("submissionId").asString()).isEqualTo(pending.toString());
    assertThat(result.get(0).path("totalCount").isNull()).isTrue();
    request("/problems/" + problem + "/submissions/mine?limit=11", student, "STUDENT", 400);
    request("/problems/" + problem + "/submissions/mine?limit=bad", student, "STUDENT", 400);
    assertThat(
            request("/problems/" + problem + "/submissions/mine?limit=1", student, "STUDENT", 200)
                .path("items"))
        .hasSize(1);
  }

  @Test
  void personalHistoryPaginatesNormalizesFiltersAndSearchesRealMetadata() throws Exception {
    for (int i = 0; i < 45; i++) {
      submission(student, problem, i % 2 == 0 ? "ACCEPTED" : "WRONG_ANSWER", time.plusSeconds(i));
    }
    submission(stranger, problem, "ACCEPTED", time);
    var page =
        request(
            "/me/submissions?page=99&verdict=bad&language=bad&userId=" + stranger,
            student,
            "STUDENT",
            200);
    assertThat(page.path("page").asInt()).isEqualTo(3);
    assertThat(page.path("items")).hasSize(5);
    assertThat(page.path("totalCount").asInt()).isEqualTo(45);
    assertThat(page.toString()).doesNotContain("private-source", "sourceCode", "testcaseDetail");
    var filtered =
        request(
            "/me/submissions?q=SUMS&verdict=ACCEPTED&language=JAVA&page=bad",
            student,
            "STUDENT",
            200);
    assertThat(filtered.path("totalCount").asInt()).isEqualTo(23);
    assertThat(filtered.path("page").asInt()).isEqualTo(1);
    assertThat(
            request("/me/submissions?q=nonexistent", student, "STUDENT", 200)
                .path("totalCount")
                .asInt())
        .isZero();
  }

  @Test
  void instructorDetailsIgnoreBreadcrumbsOmitBeatsAndUseSamePrivacyGuards() throws Exception {
    UUID c = classroom(instructor, time.minusSeconds(10), null);
    assignment(c, time.minusSeconds(5), null);
    UUID id = submission("WRONG_ANSWER");
    testcase(id, 1, false, "WA");
    var result =
        request(
            "/instructor/submissions/"
                + id
                + "?studentId="
                + stranger
                + "&classId="
                + UUID.randomUUID(),
            instructor,
            "INSTRUCTOR",
            200);
    assertThat(result.path("studentId").asString()).isEqualTo(student.toString());
    assertThat(result.path("classId").asString()).isEqualTo(c.toString());
    assertThat(result.has("beatsPercent")).isFalse();
    assertThat(result.has("beatsSampleSize")).isFalse();
    assertThat(result.path("sourceCode").asString()).isEqualTo("private-source");
    assertHidden(result.path("testcaseDetail").path("items").get(0));
    var list =
        request("/instructor/submissions?q=Two&result=failed", instructor, "INSTRUCTOR", 200);
    assertThat(list.path("items")).hasSize(1);
    assertThat(list.toString())
        .doesNotContain("sourceCode", "compileErrorMessage", "testcaseDetail");
  }

  @Test
  void instructorWindowsEnforceJoinLeaveAssignmentAndExclusiveRemoval() throws Exception {
    UUID c = classroom(instructor, time.minusSeconds(20), time.plusSeconds(10));
    assignment(c, time.minusSeconds(10), time.plusSeconds(20));
    submission(student, problem, "ACCEPTED", time.minusSeconds(11));
    UUID start = submission(student, problem, "ACCEPTED", time.minusSeconds(10));
    UUID end = submission(student, problem, "WRONG_ANSWER", time.plusSeconds(10));
    submission(student, problem, "ACCEPTED", time.plusSeconds(11));
    var rows = request("/instructor/submissions", instructor, "INSTRUCTOR", 200).path("items");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).path("submissionId").asString()).isEqualTo(end.toString());
    assertThat(rows.get(1).path("submissionId").asString()).isEqualTo(start.toString());
    assertThat(rows.get(0).path("leftClass").asBoolean()).isTrue();
    jdbc.update("UPDATE identity.class_enrollments SET left_at=NULL WHERE class_id=?", c);
    UUID removed = submission(student, problem, "ACCEPTED", time.plusSeconds(20));
    request("/instructor/submissions/" + removed, instructor, "INSTRUCTOR", 404);
    var history =
        request(
            "/instructor/classes/" + c + "/students/" + student + "/submissions",
            instructor,
            "INSTRUCTOR",
            200);
    assertThat(history.path("totalCount").asInt()).isEqualTo(3);
  }

  @Test
  void newestAssignedClassWinsBeforeClassFilterAndFixedClassHistoryKeepsItsScope()
      throws Exception {
    UUID older = classroom(instructor, time.minusSeconds(30), null);
    assignment(older, time.minusSeconds(20), null);
    UUID newer = classroom(instructor, time.minusSeconds(30), null);
    assignment(newer, time.minusSeconds(10), null);
    submission("ACCEPTED");
    var list = request("/instructor/submissions", instructor, "INSTRUCTOR", 200);
    assertThat(list.path("items")).hasSize(1);
    assertThat(list.path("items").get(0).path("classId").asString()).isEqualTo(newer.toString());
    assertThat(
            request("/instructor/submissions?classId=" + older, instructor, "INSTRUCTOR", 200)
                .path("totalCount")
                .asInt())
        .isZero();
    assertThat(
            request(
                    "/instructor/classes/" + older + "/students/" + student + "/submissions",
                    instructor,
                    "INSTRUCTOR",
                    200)
                .path("totalCount")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  void outOfScopeStudentsEmptyClassesAndUnknownIdsStayClosed() throws Exception {
    UUID id = submission("ACCEPTED");
    var absent =
        request("/instructor/submissions/" + UUID.randomUUID(), instructor, "INSTRUCTOR", 404);
    sameNotFound(request("/instructor/submissions/" + id, instructor, "INSTRUCTOR", 404), absent);
    request("/instructor/submissions", student, "STUDENT", 403);
    request("/instructor/submissions?classId=bad", instructor, "INSTRUCTOR", 404);
    UUID other = classroom(user("INSTRUCTOR"), time.minusSeconds(10), null);
    request(
        "/instructor/classes/" + other + "/students/" + student + "/submissions",
        instructor,
        "INSTRUCTOR",
        404);
    UUID mine = classroom(instructor, time.minusSeconds(10), null);
    request(
        "/instructor/classes/" + mine + "/students/" + stranger + "/submissions",
        instructor,
        "INSTRUCTOR",
        404);
    jdbc.update("DELETE FROM identity.class_enrollments WHERE class_id=?", mine);
    assertThat(
            request("/instructor/submissions?classId=" + mine, instructor, "INSTRUCTOR", 200)
                .path("totalCount")
                .asInt())
        .isZero();
  }

  @Test
  void permissionRevocationIsLiveForAnAlreadyIssuedToken() throws Exception {
    String bearer = token(instructor, "INSTRUCTOR");
    String sql =
        """
        UPDATE identity.permissions SET granted=? WHERE role_id=(SELECT id FROM identity.roles WHERE code='INSTRUCTOR')
        AND function_id=(SELECT id FROM identity.functions WHERE code='CLASS_MANAGEMENT')
        AND action_id=(SELECT id FROM identity.actions WHERE code='READ')
        """;
    assertThat(
            mvc.perform(
                    get("/api/v1/judge/instructor/submissions")
                        .header("Authorization", "Bearer " + bearer))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
    jdbc.update(sql, false);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='INSTRUCTOR'");
    try {
      var response =
          mvc.perform(
                  get("/api/v1/judge/instructor/submissions")
                      .header("Authorization", "Bearer " + bearer))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).isEqualTo(403);
      assertThat(json.readTree(response.getContentAsString()).path("errorCode").asString())
          .isEqualTo("JDG-301");
    } finally {
      jdbc.update(sql, true);
      jdbc.update(
          "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='INSTRUCTOR'");
    }
  }

  @Test
  void scopeAndSearchFailuresReturnApprovedReadUnavailableCodeWithoutInternalMessages()
      throws Exception {
    doThrow(new IllegalStateException("secret-database")).when(scope).read(any());
    var failure = request("/instructor/submissions", instructor, "INSTRUCTOR", 503);
    assertThat(failure.path("errorCode").asString()).isEqualTo("JDG-506");
    assertThat(failure.toString()).doesNotContain("secret-database");
    doThrow(new IllegalStateException("secret-search"))
        .when(metadata)
        .search(any(), org.mockito.ArgumentMatchers.anyInt());
    assertThat(
            request("/me/submissions?q=Two", student, "STUDENT", 503).path("errorCode").asString())
        .isEqualTo("JDG-506");
  }

  @Test
  void progressSnapshotCannotMixOldStatusWithANewerCommittedTestcase() throws Exception {
    UUID id = submission("JUDGING");
    doAnswer(
            invocation -> {
              Object state = invocation.callRealMethod();
              try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> testcase(id, 1, true, "WA")).get();
              }
              return state;
            })
        .when(repository)
        .owned(id, student);
    var snapshot = request("/submissions/" + id + "/progress", student, "STUDENT", 200);
    assertThat(snapshot.path("completedCount").asInt()).isZero();
    assertThat(snapshot.path("seq").asInt()).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
                Integer.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void reconnectSnapshotsMatchActualWorkerEventsAndKeepOnlyOrderVerdict() throws Exception {
    UUID id = submission("PENDING");
    compile.begin(id, 1);
    judging.beginJudging(id, 2);
    recordWorkerCase(id, 1, "WA");
    var during = request("/submissions/" + id + "/progress", student, "STUDENT", 200);
    assertThat(during.path("seq").asInt()).isEqualTo(Recording.EVENTS.getLast().seq());
    recordWorkerCase(id, 2, "AC");
    finalizer.finish(new FinishCommand(id, FinishKind.COMPLETE, null, null));
    var finished = request("/submissions/" + id + "/progress", student, "STUDENT", 200);
    assertThat(finished.path("seq").asInt())
        .isEqualTo(Recording.EVENTS.getLast().seq())
        .isEqualTo(5);
    assertThat(finished.path("isFinal").asBoolean()).isTrue();
    assertThat(finished.path("firstFailingOrder").asInt()).isEqualTo(1);
    assertThat(finished.path("testcases")).hasSize(2);
    assertThat(finished.path("testcases").get(0).size()).isEqualTo(2);
    assertThat(finished.toString())
        .doesNotContain("runtimeMs", "memoryKb", "source", "stderr", "actualOutput");
    assertThat(request("/submissions/" + id + "/progress", student, "STUDENT", 200))
        .isEqualTo(finished);
  }

  private void recordWorkerCase(UUID id, int order, String verdict) {
    UUID testcase = UUID.randomUUID();
    var issued = tokens.issue(id, testcase).orElseThrow();
    results.record(
        new TestcaseWrite(
            id, testcase, issued.id(), order, false, verdict, 100, 256, null, null, false));
  }

  private static void sameNotFound(JsonNode actual, JsonNode expected) {
    // RFC 9457 instance echoes the requested URI, which the caller already knows.
    for (String field : List.of("status", "title", "detail", "errorCode")) {
      assertThat(actual.path(field)).isEqualTo(expected.path(field));
    }
  }

  @Test
  void joinBoundaryRemovalsAndSelfPracticeAreExcludedIndependently() throws Exception {
    UUID c = classroom(instructor, time.minusSeconds(20), null);
    assignment(c, time.minusSeconds(30), time.plusSeconds(10));
    UUID early = submission(student, problem, "ACCEPTED", time.minusSeconds(21));
    UUID joined = submission(student, problem, "ACCEPTED", time.minusSeconds(20));
    UUID removed = submission(student, problem, "ACCEPTED", time.plusSeconds(10));
    UUID practice = submission(student, problem("Self practice"), "ACCEPTED", time);
    var list = request("/instructor/submissions", instructor, "INSTRUCTOR", 200);
    assertThat(list.path("items")).hasSize(1);
    assertThat(list.path("items").get(0).path("submissionId").asString())
        .isEqualTo(joined.toString());
    for (UUID denied : List.of(early, removed, practice)) {
      request("/instructor/submissions/" + denied, instructor, "INSTRUCTOR", 404);
    }
  }

  @Test
  void instructorPaginationAndResultFiltersKeepSystemErrorsOutOfFailed() throws Exception {
    UUID c = classroom(instructor, time.minusSeconds(20), null);
    assignment(c, time.minusSeconds(10), null);
    for (int i = 0; i < 25; i++) {
      submission("WRONG_ANSWER");
    }
    submission("SYSTEM_ERROR");
    submission("PENDING");
    submission("ACCEPTED");
    var failed =
        request("/instructor/submissions?result=failed&page=99", instructor, "INSTRUCTOR", 200);
    assertThat(failed.path("totalCount").asInt()).isEqualTo(25);
    assertThat(failed.path("page").asInt()).isEqualTo(2);
    assertThat(failed.path("items")).hasSize(5);
    assertThat(
            request(
                    "/instructor/submissions?result=unknown&q=Learner",
                    instructor,
                    "INSTRUCTOR",
                    200)
                .path("totalCount")
                .asInt())
        .isEqualTo(28);
    var history =
        request(
            "/instructor/classes/" + c + "/students/" + student + "/submissions?page=99",
            instructor,
            "INSTRUCTOR",
            200);
    assertThat(history.path("items")).hasSize(8);
    assertThat(history.toString()).doesNotContain("sourceCode", "testcaseDetail");
  }

  @Test
  void partialSystemErrorProgressKeepsTheFinalEventSequenceAndNullScore() throws Exception {
    UUID id = submission("PENDING");
    compile.begin(id, 1);
    judging.beginJudging(id, 2);
    recordWorkerCase(id, 1, "WA");
    finalizer.finish(new FinishCommand(id, FinishKind.SYSTEM_ERROR, "WORKER_TIMEOUT", null));
    var result = request("/submissions/" + id + "/progress", student, "STUDENT", 200);
    assertThat(result.path("seq").asInt()).isEqualTo(Recording.EVENTS.getLast().seq()).isEqualTo(5);
    assertThat(result.path("completedCount").asInt()).isEqualTo(1);
    assertThat(result.path("scoreRatio").isNull()).isTrue();
    assertThat(result.path("firstFailingOrder").isNull()).isTrue();
  }
}
