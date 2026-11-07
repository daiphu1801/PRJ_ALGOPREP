package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
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
 * Personal data export #16 with {@code scope=submissions} (DD api F1 section 0.3), against real
 * PostgreSQL and Redis with real tokens. The rows come through the real bridge and the real F4
 * history port, so the data is seeded in {@code judge.submissions} and {@code problem.problems}.
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
class SubmissionExportFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String SOURCE_MARKER = "SECRET_SOURCE_MARKER_91C";

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
  @Autowired private StringRedisTemplate redis;
  private MockMvc mvc;
  private UUID author;
  private UUID problem;
  private UUID alice;
  private UUID bob;
  private String aliceToken;

  @BeforeEach
  void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update("DELETE FROM problem.problems");
    redis.delete(redis.keys("identity:export:*"));
    String authorEmail = account("INSTRUCTOR");
    author =
        jdbc.queryForObject(
            "SELECT id FROM identity.users WHERE email = ?", UUID.class, authorEmail);
    problem = problem();
    String aliceEmail = account("STUDENT");
    alice =
        jdbc.queryForObject(
            "SELECT id FROM identity.users WHERE email = ?", UUID.class, aliceEmail);
    bob =
        jdbc.queryForObject(
            "SELECT id FROM identity.users WHERE email = ?", UUID.class, account("STUDENT"));
    aliceToken = login(aliceEmail);
  }

  @Test
  void theFileHoldsOnlyTheCallersOwnRowsWithoutSourceCodeAndIgnoresAUserIdParameter()
      throws Exception {
    List<UUID> own =
        List.of(
            submission(alice, "ACCEPTED", "3 minutes"),
            submission(alice, "WRONG_ANSWER", "2 minutes"));
    UUID foreign = submission(bob, "ACCEPTED", "1 minutes");

    MockHttpServletResponse response = export(aliceToken, "&userId=" + bob);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Content-Disposition")).contains("algoprep-submissions.csv");
    assertThat(response.getContentType()).startsWith("text/csv");
    String body = response.getContentAsString();
    assertThat(body).contains(own.get(0).toString(), own.get(1).toString());
    assertThat(body).doesNotContain(foreign.toString()).doesNotContain(SOURCE_MARKER);
    List<String> lines = lines(body);
    assertThat(lines.get(0))
        .contains(
            "submission_id,submitted_at,problem_code,problem_title,language,status,"
                + "passed_count,total_count,runtime_ms");
    assertThat(lines).hasSize(3);
    assertThat(lines.get(1)).contains(own.get(1).toString(), "WRONG_ANSWER", "Export problem");
    assertThat(lines.get(2)).contains("ACCEPTED", ",3,3,12");
    assertThat(response.getHeader("X-Export-Truncated")).isNull();
  }

  @Test
  void aCallerWithNoSubmissionsGetsAHeaderOnlyFile() throws Exception {
    MockHttpServletResponse response = export(aliceToken, "");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(lines(response.getContentAsString())).hasSize(1);
  }

  @Test
  void theLoopReadsEveryPageOfTheHistory() throws Exception {
    insertMany(alice, 45);
    submission(bob, "ACCEPTED", "1 minutes");

    MockHttpServletResponse response = export(aliceToken, "");

    assertThat(response.getStatus()).isEqualTo(200);
    List<String> lines = lines(response.getContentAsString());
    assertThat(lines).hasSize(46);
    assertThat(lines.stream().skip(1).distinct()).hasSize(45);
  }

  @Test
  void moreThanTheCapIsCutAndSaidSo() throws Exception {
    insertMany(alice, 2001);

    MockHttpServletResponse response = export(aliceToken, "");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(lines(response.getContentAsString())).hasSize(2001);
    assertThat(response.getHeader("X-Export-Truncated")).isEqualTo("true");
  }

  @Test
  void aSecondExportWithinFiveMinutesIs429ButTheProfileFileHasItsOwnSlot() throws Exception {
    assertThat(export(aliceToken, "").getStatus()).isEqualTo(200);

    MockHttpServletResponse again = export(aliceToken, "");

    assertThat(again.getStatus()).isEqualTo(429);
    assertThat(JsonPath.<String>read(again.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V06");
    assertThat(call("profile", aliceToken, "").getStatus()).isEqualTo(200);
  }

  @Test
  void withoutATokenTheAnswerIs401() throws Exception {
    assertThat(
            mvc.perform(get("/api/v1/identity/me/export?scope=submissions"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void aJudgeFailureIs503WithoutDetailAndTheSlotIsGivenBack() throws Exception {
    submission(alice, "ACCEPTED", "1 minutes");
    jdbc.execute("ALTER TABLE judge.submissions RENAME TO submissions_away");
    try {
      MockHttpServletResponse failed = export(aliceToken, "");

      assertThat(failed.getStatus()).isEqualTo(503);
      assertThat(failed.getContentAsString())
          .contains("IDT-508")
          .doesNotContain("judge.", "relation");
    } finally {
      jdbc.execute("ALTER TABLE judge.submissions_away RENAME TO submissions");
    }

    MockHttpServletResponse retry = export(aliceToken, "");

    assertThat(retry.getStatus()).isEqualTo(200);
    assertThat(lines(retry.getContentAsString())).hasSize(2);
  }

  private static List<String> lines(String csv) {
    return csv.replace("﻿", "").lines().toList();
  }

  private UUID problem() {
    UUID id = UUID.randomUUID();
    UUID level = jdbc.queryForObject("SELECT id FROM problem.problem_levels LIMIT 1", UUID.class);
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb,status,deleted)"
            + " VALUES (?,?,?,?,?,?,1000,256,64,'PUBLISHED',false)",
        id,
        "EXP-" + id.toString().substring(0, 8),
        "Export problem",
        author,
        author,
        level);
    return id;
  }

  private UUID submission(UUID user, String status, String ago) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,passed_testcase_count,total_testcase_count,runtime_ms,
          testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO',?,'EDITOR',?,3,3,12,1,
          now() - CAST(? AS interval), now() - CAST(? AS interval))
        """,
        id,
        user,
        problem,
        SOURCE_MARKER,
        status,
        ago,
        ago);
    return id;
  }

  private void insertMany(UUID user, int count) {
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,passed_testcase_count,total_testcase_count,runtime_ms,
          testcase_set_version_at_grading,submitted_at,finished_at)
        SELECT gen_random_uuid(),?,?,'PYTHON','STANDARD_IO',?,'EDITOR','ACCEPTED',1,1,5,1,
          now() - (g * interval '1 second'), now() - (g * interval '1 second')
        FROM generate_series(1, ?) g
        """,
        user,
        problem,
        SOURCE_MARKER,
        count);
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

  private MockHttpServletResponse export(String token, String extraQuery) throws Exception {
    return call("submissions", token, extraQuery);
  }

  private MockHttpServletResponse call(String scope, String token, String extraQuery)
      throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/export?scope=" + scope + extraQuery)
                .header("Authorization", "Bearer " + token))
        .andReturn()
        .getResponse();
  }
}
