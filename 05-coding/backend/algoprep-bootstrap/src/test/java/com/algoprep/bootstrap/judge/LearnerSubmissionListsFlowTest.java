package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.ports.out.JudgeConfigRepository;
import com.algoprep.judge.domain.ports.out.LearnerSubmissionListRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
 * JWT/current authorities, real PostgreSQL and real F2 metadata bridge; no outer test transaction.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class LearnerSubmissionListsFlowTest {
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
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired WebApplicationContext context;
  @MockitoSpyBean ProblemSummaryReadPort problems;
  @MockitoSpyBean LearnerSubmissionListRepository repository;
  @MockitoSpyBean JudgeConfigRepository config;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private MockMvc mvc;
  private UUID student;
  private Instant now;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    student = user("STUDENT");
    now =
        jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class)
            .toInstant()
            .minusSeconds(5);
    jdbc.update("UPDATE judge.language_configs SET enabled=true");
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'List user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private UUID problem(boolean withdrawn) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES(?,?,?,?,'20000000-0000-4000-8000-000000000001',?,false,1000,256,64)",
        id,
        "LIST-" + id,
        "List problem " + id,
        student,
        withdrawn ? "UNPUBLISHED" : "PUBLISHED");
    return id;
  }

  private UUID submission(UUID owner, UUID problem, int ago, String status) {
    UUID id = UUID.randomUUID();
    fixedSubmission(id, owner, problem, now.minusSeconds(ago), status);
    return id;
  }

  private void fixedSubmission(UUID id, UUID owner, UUID problem, Instant at, String status) {
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,passed_testcase_count,total_testcase_count,
          testcase_set_version_at_grading,runtime_ms,memory_kb,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source-secret','EDITOR',?,?,1,2,1,NULL,0,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        id,
        owner,
        problem,
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        at.toString(),
        terminal ? at.toString() : null);
  }

  private JsonNode request(String path, UUID actor, BaseCategory role, int status)
      throws Exception {
    var response =
        mvc.perform(
                get("/api/v1/judge" + path)
                    .header(
                        "Authorization",
                        "Bearer "
                            + tokens.issue(actor, role, role.name(), UUID.randomUUID()).value()))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode own(String path) throws Exception {
    return request(path, student, BaseCategory.STUDENT, 200).path("items");
  }

  private static void privateFieldsAbsent(JsonNode items) {
    assertThat(items.toString())
        .doesNotContain(
            "private-source-secret",
            "sourceCode",
            "testcase",
            "stderr",
            "compileError",
            "expectedOutput",
            "diff",
            "userId");
  }

  @Test
  void emptyListsAndAuthenticatedLanguageReadHaveExactPublicShapes() throws Exception {
    assertThat(own("/me/in-progress-problems").size()).isZero();
    assertThat(own("/me/accepted-submissions").size()).isZero();
    var languages = own("/languages/enabled");
    assertThat(languages.toString()).isEqualTo("[\"PYTHON\",\"JAVA\",\"CPP\"]");
  }

  @Test
  void progressUsesLatestOwnSubmissionIncludingAcThenFailureAndInflight() throws Exception {
    UUID solved = problem(false);
    UUID reopened = problem(false);
    UUID wrong = problem(false);
    UUID pending = problem(false);
    UUID system = problem(false);
    submission(student, solved, 60, "WRONG_ANSWER");
    submission(student, solved, 20, "ACCEPTED");
    submission(student, reopened, 60, "ACCEPTED");
    UUID reopenedId = submission(student, reopened, 10, "WRONG_ANSWER");
    submission(student, wrong, 15, "WRONG_ANSWER");
    submission(student, pending, 5, "PENDING");
    submission(student, system, 3, "SYSTEM_ERROR");
    submission(user("STUDENT"), problem(false), 1, "WRONG_ANSWER");
    var defaults = own("/me/in-progress-problems?userId=" + UUID.randomUUID());
    assertThat(defaults.size()).isEqualTo(3);
    var all = own("/me/in-progress-problems?limit=20");
    assertThat(all.size()).isEqualTo(4);
    assertThat(all.toString()).contains(reopenedId.toString()).doesNotContain(solved.toString());
    assertThat(all.get(0).path("problem").size()).isEqualTo(5);
    assertThat(all.get(0).path("problem").path("withdrawn").asBoolean()).isFalse();
    assertThat(all.get(0).path("passedCount").isNull()).isTrue();
    privateFieldsAbsent(all);
  }

  @Test
  void withdrawnRowsDoNotConsumeLimitEvenBeyondTheFirstMetadataBatch() throws Exception {
    for (int i = 0; i < 33; i++) {
      submission(student, problem(true), i, "WRONG_ANSWER");
    }
    UUID first = submission(student, problem(false), 100, "WRONG_ANSWER");
    submission(student, problem(false), 101, "PENDING");
    submission(student, problem(false), 102, "WRONG_ANSWER");
    var items = own("/me/in-progress-problems");
    assertThat(items.size()).isEqualTo(3);
    assertThat(items.get(0).path("latestSubmissionId").asText()).isEqualTo(first.toString());
    assertThat(own("/me/in-progress-problems?limit=1").size()).isEqualTo(1);
    assertThat(own("/me/in-progress-problems?limit=20").size()).isEqualTo(3);
  }

  @Test
  void acceptedListsOwnRepeatedAcceptedRowsIncludingWithdrawnProblemsWithNoSource()
      throws Exception {
    UUID old = problem(true);
    UUID active = problem(false);
    for (int i = 0; i < 12; i++) {
      submission(student, i % 2 == 0 ? old : active, i, "ACCEPTED");
    }
    submission(student, active, 0, "WRONG_ANSWER");
    submission(student, active, 0, "SYSTEM_ERROR");
    submission(user("STUDENT"), active, 0, "ACCEPTED");
    assertThat(own("/me/accepted-submissions").size()).isEqualTo(10);
    var items = own("/me/accepted-submissions?limit=20&userId=" + UUID.randomUUID());
    assertThat(items.size()).isEqualTo(12);
    assertThat(items.get(0).path("problem").size()).isEqualTo(4);
    assertThat(items.get(0).path("runtimeMs").isNull()).isTrue();
    assertThat(items.get(0).path("memoryKb").asInt()).isZero();
    assertThat(own("/me/accepted-submissions?limit=1").size()).isEqualTo(1);
    privateFieldsAbsent(items);
  }

  @Test
  void tiedSubmissionTimesUsePostgresUuidOrderingForLatestAndAcceptedLists() throws Exception {
    UUID p = problem(false);
    UUID low = UUID.fromString("7fffffff-0000-0000-0000-000000000002");
    UUID high = UUID.fromString("80000000-0000-0000-0000-000000000002");
    fixedSubmission(low, student, p, now, "WRONG_ANSWER");
    fixedSubmission(high, student, p, now, "ACCEPTED");
    assertThat(own("/me/in-progress-problems").size()).isZero();
    UUID other = UUID.fromString("7fffffff-0000-0000-0000-000000000003");
    fixedSubmission(other, student, p, now, "ACCEPTED");
    var items = own("/me/accepted-submissions");
    assertThat(items.get(0).path("submissionId").asText()).isEqualTo(high.toString());
    assertThat(items.get(1).path("submissionId").asText()).isEqualTo(other.toString());
  }

  @Test
  void optionalMetadataFailurePreservesRowsWithExplicitNullAndNoFabricatedLinks() throws Exception {
    UUID p = problem(false);
    submission(student, p, 5, "WRONG_ANSWER");
    submission(student, problem(false), 6, "ACCEPTED");
    doThrow(new IllegalStateException("private F2 SQL password")).when(problems).summaries(any());
    var progress = own("/me/in-progress-problems");
    var accepted = own("/me/accepted-submissions");
    assertThat(progress.size()).isEqualTo(1);
    assertThat(accepted.size()).isEqualTo(1);
    assertThat(progress.get(0).path("problem").isNull()).isTrue();
    assertThat(accepted.get(0).path("problem").isNull()).isTrue();
    assertThat(progress.toString() + accepted).doesNotContain("private F2", "password");
  }

  @Test
  void roleAndLimitBoundariesAreEnforcedByRealJwtRequests() throws Exception {
    UUID instructor = user("INSTRUCTOR");
    UUID admin = user("ADMIN");
    for (String path :
        List.of("/me/in-progress-problems", "/me/accepted-submissions", "/languages/enabled")) {
      assertThat(mvc.perform(get("/api/v1/judge" + path)).andReturn().getResponse().getStatus())
          .isEqualTo(401);
      request(path, admin, BaseCategory.ADMIN, 403);
    }
    for (String path : List.of("/me/in-progress-problems", "/me/accepted-submissions")) {
      request(path, instructor, BaseCategory.INSTRUCTOR, 403);
      for (String limit : List.of("0", "21", "-1", "bad", "1.5", "2147483648", "")) {
        var error = request(path + "?limit=" + limit, student, BaseCategory.STUDENT, 400);
        assertThat(error.path("errorCode").asText()).isEqualTo("JDG-V14");
      }
    }
    request("/languages/enabled", instructor, BaseCategory.INSTRUCTOR, 200);
  }

  @Test
  void databaseFailuresAreGeneric503WithoutFalseZerosOrReusingReservedCodes() throws Exception {
    doThrow(new IllegalStateException("private SQL password"))
        .when(repository)
        .inProgress(any(), any(), anyInt());
    doThrow(new IllegalStateException("private SQL password"))
        .when(repository)
        .accepted(any(), anyInt());
    doThrow(new IllegalStateException("private SQL password")).when(config).enabledLanguages();
    for (String path :
        List.of("/me/in-progress-problems", "/me/accepted-submissions", "/languages/enabled")) {
      var error = request(path, student, BaseCategory.STUDENT, 503);
      assertThat(error.path("status").asInt()).isEqualTo(503);
      assertThat(error.has("items")).isFalse();
      assertThat(error.has("errorCode")).isFalse();
      assertThat(error.toString()).doesNotContain("private SQL", "password", "JDG-506", "JDG-507");
    }
  }

  @Test
  void enabledLanguageOrderAndChangesAreVisibleToStudentsAndInstructorsWithoutAdminData()
      throws Exception {
    UUID instructor = user("INSTRUCTOR");
    jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='JAVA'");
    var own = own("/languages/enabled");
    assertThat(own.toString()).isEqualTo("[\"PYTHON\",\"CPP\"]");
    var teacher = request("/languages/enabled", instructor, BaseCategory.INSTRUCTOR, 200);
    assertThat(teacher.path("items")).isEqualTo(own);
    assertThat(teacher.size()).isEqualTo(1);
    jdbc.update("UPDATE judge.language_configs SET enabled=false");
    request("/languages/enabled", student, BaseCategory.STUDENT, 503);
  }
}
