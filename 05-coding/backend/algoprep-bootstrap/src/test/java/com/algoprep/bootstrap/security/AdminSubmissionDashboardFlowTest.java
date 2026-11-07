package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

/**
 * The seven judge-backed blocks of the admin overview, #43 and #67 to #72 (DD api F1 section
 * 0.8.5), against real PostgreSQL and Redis with real tokens. The judge and problem-bank answers
 * come through the real bridges, so the data is seeded in {@code judge.submissions} and {@code
 * problem.problems}.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=8"
    })
@ActiveProfiles("test")
@Testcontainers
class AdminSubmissionDashboardFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/admin/dashboard";
  private static final ZoneId LOCAL = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final List<String> PATHS =
      List.of(
          BASE + "/cross-module",
          BASE + "/submissions/by-language",
          BASE + "/submissions/verdicts",
          BASE + "/submissions/difficulty",
          BASE + "/submissions/by-day",
          BASE + "/submissions/by-month",
          BASE + "/top-problems");

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

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;
  private UUID author;
  private String admin;

  @BeforeEach
  void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update("DELETE FROM problem.problems");
    String authorEmail = account("INSTRUCTOR");
    author =
        jdbc.queryForObject(
            "SELECT id FROM identity.users WHERE email = ?", UUID.class, authorEmail);
    admin = login(account("ADMIN"));
  }

  @Test
  void everyBlockRefusesAnonymousStudentAndInstructorCallers() throws Exception {
    String student = login(account("STUDENT"));
    String instructor = login(account("INSTRUCTOR"));
    for (String path : PATHS) {
      assertThat(mvc.perform(get(path)).andReturn().getResponse().getStatus()).isEqualTo(401);
      assertThat(call(path, student).getStatus()).isEqualTo(403);
      assertThat(call(path, instructor).getStatus()).isEqualTo(403);
    }
  }

  @Test
  void noSubmissionsGivesHonestEmptyShapesForEveryBlock() throws Exception {
    String summary = call(BASE + "/cross-module", admin).getContentAsString();
    assertThat(JsonPath.<Integer>read(summary, "$.value")).isZero();
    assertThat(JsonPath.<Object>read(summary, "$.deltaPercent")).isNull();
    assertThat(JsonPath.<String>read(summary, "$.deltaDirection")).isEqualTo("none");

    String language = call(BASE + "/submissions/by-language", admin).getContentAsString();
    assertThat(JsonPath.<List<String>>read(language, "$.pointLabels")).hasSize(20);
    assertThat(JsonPath.<List<String>>read(language, "$.series[*].languageCode"))
        .containsExactly("JAVA", "CPP", "PYTHON");

    String verdicts = call(BASE + "/submissions/verdicts", admin).getContentAsString();
    assertThat(JsonPath.<List<Integer>>read(verdicts, "$.slices[*].percent")).containsOnly(0);

    String days = call(BASE + "/submissions/by-day", admin).getContentAsString();
    assertThat(JsonPath.<List<Object>>read(days, "$.columns")).hasSize(7);
    assertThat(JsonPath.<Integer>read(days, "$.dailyAverage")).isZero();

    String months = call(BASE + "/submissions/by-month", admin).getContentAsString();
    assertThat(JsonPath.<List<Object>>read(months, "$.points")).hasSize(6);

    String top = call(BASE + "/top-problems", admin).getContentAsString();
    assertThat(JsonPath.<List<Object>>read(top, "$.items")).isEmpty();
  }

  @Test
  void theCountBlocksReadTheRealSubmissionsInUtcPlusSevenAndStayInsideTheirWindows()
      throws Exception {
    UUID problem = problem("PUBLISHED", false, levelId(0));
    submission(problem, "5 minutes", "ACCEPTED", "JAVA");
    submission(problem, "5 minutes", "ACCEPTED", "JAVA");
    submission(problem, "5 minutes", "WRONG_ANSWER", "CPP");
    submission(problem, "5 minutes", "SYSTEM_ERROR", "PYTHON");
    submission(problem, "5 minutes", "PENDING", "PYTHON");
    submission(problem, "3 days", "COMPILE_ERROR", "PYTHON");
    submission(problem, "400 days", "ACCEPTED", "JAVA");

    String summary = call(BASE + "/cross-module", admin).getContentAsString();
    assertThat(JsonPath.<Integer>read(summary, "$.value")).isEqualTo(7);
    assertThat(JsonPath.<List<Integer>>read(summary, "$.sparklineSeries")).hasSize(7);

    String verdicts = call(BASE + "/submissions/verdicts", admin).getContentAsString();
    // Final verdicts only: SYSTEM_ERROR and PENDING are left out, the 400-day row still counts.
    assertThat(JsonPath.<List<String>>read(verdicts, "$.slices[*].verdict"))
        .containsExactly(
            "ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "COMPILE_ERROR");
    assertThat(JsonPath.<Integer>read(verdicts, "$.slices[0].count")).isEqualTo(3);
    assertThat(
            JsonPath.<List<Integer>>read(verdicts, "$.slices[*].percent").stream()
                .mapToInt(Integer::intValue)
                .sum())
        .isEqualTo(100);

    String days = call(BASE + "/submissions/by-day", admin).getContentAsString();
    assertThat(JsonPath.<Integer>read(days, "$.weekTotal")).isEqualTo(6);
    assertThat(JsonPath.<String>read(days, "$.columns[6].day"))
        .isEqualTo(LocalDate.now(LOCAL).toString());
    assertThat(JsonPath.<Integer>read(days, "$.dailyAverage")).isEqualTo(1);

    String months = call(BASE + "/submissions/by-month", admin).getContentAsString();
    assertThat(JsonPath.<String>read(months, "$.points[5].month"))
        .isEqualTo(YearMonth.now(LOCAL).toString());
    assertThat(JsonPath.<Integer>read(months, "$.monthTotal")).isBetween(5, 6);

    String language = call(BASE + "/submissions/by-language", admin).getContentAsString();
    assertThat(JsonPath.<Integer>read(language, "$.series[0].values[19]")).isEqualTo(2);
    assertThat(JsonPath.<Integer>read(language, "$.series[1].values[19]")).isEqualTo(1);
  }

  @Test
  void difficultyHasOneGroupPerLevelInCatalogueOrderAndCountsOnlyPublishedProblems()
      throws Exception {
    UUID easy = problem("PUBLISHED", false, levelId(0));
    UUID withdrawn = problem("UNPUBLISHED", false, levelId(0));
    UUID deleted = problem("UNPUBLISHED", true, levelId(1));
    submission(easy, "1 hour", "ACCEPTED", "JAVA");
    submission(easy, "1 hour", "WRONG_ANSWER", "JAVA");
    submission(withdrawn, "1 hour", "ACCEPTED", "JAVA");
    submission(deleted, "1 hour", "ACCEPTED", "JAVA");

    String body = call(BASE + "/submissions/difficulty", admin).getContentAsString();

    List<String> codes =
        jdbc.queryForList(
            "SELECT code FROM problem.problem_levels ORDER BY sort_order, code", String.class);
    assertThat(JsonPath.<List<String>>read(body, "$.groups[*].levelCode")).isEqualTo(codes);
    assertThat(JsonPath.<Integer>read(body, "$.groups[0].submissions")).isEqualTo(2);
    assertThat(JsonPath.<Integer>read(body, "$.groups[0].accepted")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.groups[1].submissions")).isZero();
    assertThat(JsonPath.<Integer>read(body, "$.groups[1].accepted")).isZero();
    assertThat(JsonPath.<List<String>>read(body, "$.legend"))
        .containsExactly("totalSubmissions", "acceptedSubmissions");
  }

  @Test
  void topProblemsAreFourPublishedOnesByCountWithTheirTitlesAndNeverAWithdrawnOne()
      throws Exception {
    UUID withdrawn = problem("UNPUBLISHED", false, levelId(0));
    for (int i = 0; i < 9; i++) {
      submission(withdrawn, "1 hour", "ACCEPTED", "JAVA");
    }
    UUID[] published = new UUID[5];
    for (int i = 0; i < 5; i++) {
      published[i] = problem("PUBLISHED", false, levelId(0));
      for (int n = 0; n <= i; n++) {
        submission(published[i], "1 hour", "ACCEPTED", "CPP");
      }
    }

    String body = call(BASE + "/top-problems", admin).getContentAsString();

    assertThat(JsonPath.<List<String>>read(body, "$.items[*].problemId"))
        .containsExactly(
            published[4].toString(),
            published[3].toString(),
            published[2].toString(),
            published[1].toString());
    assertThat(JsonPath.<List<Integer>>read(body, "$.items[*].submissionCount"))
        .containsExactly(5, 4, 3, 2);
    assertThat(JsonPath.<String>read(body, "$.items[0].title")).isEqualTo("Title " + published[4]);
  }

  @Test
  void oneSourceFailingIsA503ForItsOwnBlockOnlyAndTheErrorCarriesNoDetail() throws Exception {
    problem("PUBLISHED", false, levelId(0));
    jdbc.execute("ALTER TABLE problem.problem_levels RENAME TO problem_levels_away");
    try {
      MockHttpServletResponse difficulty = call(BASE + "/submissions/difficulty", admin);
      MockHttpServletResponse top = call(BASE + "/top-problems", admin);
      MockHttpServletResponse days = call(BASE + "/submissions/by-day", admin);

      assertThat(difficulty.getStatus()).isEqualTo(503);
      assertThat(difficulty.getContentAsString())
          .contains("IDT-506")
          .doesNotContain("problem_levels");
      // The published list joins the levels, so top problems fails too; judge-only blocks do not.
      assertThat(top.getStatus()).isEqualTo(503);
      assertThat(days.getStatus()).isEqualTo(200);
    } finally {
      jdbc.execute("ALTER TABLE problem.problem_levels_away RENAME TO problem_levels");
    }
  }

  private UUID levelId(int index) {
    return jdbc.queryForObject(
        "SELECT id FROM problem.problem_levels ORDER BY sort_order, code OFFSET ? LIMIT 1",
        UUID.class,
        index);
  }

  private UUID problem(String status, boolean deleted, UUID level) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb,status,deleted)"
            + " VALUES (?,?,?,?,?,?,1000,256,64,?,?)",
        id,
        id.toString(),
        "Title " + id,
        author,
        author,
        level,
        status,
        deleted);
    return id;
  }

  private void submission(UUID problem, String ago, String status, String language) {
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,score_ratio,testcase_set_version_at_grading,
          submitted_at,finished_at)
        VALUES(?,?,?,?,'STANDARD_IO','x','EDITOR',?,?,?,1,
          now() - CAST(? AS interval),
          CASE WHEN ? THEN now() - CAST(? AS interval) END)
        """,
        UUID.randomUUID(),
        UUID.randomUUID(),
        problem,
        language,
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        status.equals("ACCEPTED") ? java.math.BigDecimal.ONE : null,
        ago,
        terminal,
        ago);
  }

  private String account(String roleCode) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'QAUser123',id,'ACTIVE',false"
            + " FROM identity.roles WHERE code = ?",
        UUID.randomUUID(),
        email,
        HASH,
        roleCode);
    return email;
  }

  private String login(String email) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post("/api/v1/identity/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JsonPath.read(response.getContentAsString(), "$.accessToken");
  }

  private MockHttpServletResponse call(String path, String token) throws Exception {
    return mvc.perform(get(path).header("Authorization", "Bearer " + token))
        .andReturn()
        .getResponse();
  }
}
