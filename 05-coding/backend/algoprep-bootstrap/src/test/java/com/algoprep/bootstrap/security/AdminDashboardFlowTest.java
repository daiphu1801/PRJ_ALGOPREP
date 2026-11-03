package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
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
 * The identity cards of the admin overview, #41, #42 and #73 (DD api F1 section 0.8.5), against
 * real PostgreSQL and Redis with real tokens from {@code /auth/login}.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class AdminDashboardFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String BASE = "/api/v1/identity/admin/dashboard";
  private static final List<String> PATHS =
      List.of(BASE + "/summary", BASE + "/user-growth", BASE + "/password-resets");

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

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM identity.refresh_tokens");
    jdbc.update(
        "DELETE FROM identity.system_audit_logs WHERE action_type = 'PASSWORD_RESET_REQUESTED'");
  }

  @Test
  void everyCardRefusesAnonymousStudentAndInstructorCallers() throws Exception {
    String student = login(account("STUDENT", "ACTIVE"));
    String instructor = login(account("INSTRUCTOR", "ACTIVE"));
    for (String path : PATHS) {
      assertThat(mvc.perform(get(path)).andReturn().getResponse().getStatus()).isEqualTo(401);
      assertThat(call(path, student).getStatus()).isEqualTo(403);
      assertThat(call(path, instructor).getStatus()).isEqualTo(403);
    }
  }

  @Test
  void activeUsersCountThirtyDaysAgainstTheThirtyBeforeAndIgnoreDeactivatedAccounts()
      throws Exception {
    String admin = login(account("ADMIN", "ACTIVE")); // one token issued now
    token(userId(account("STUDENT", "ACTIVE")), "5 days"); // counts now
    token(userId(account("STUDENT", "ACTIVE")), "40 days"); // the period before
    token(userId(account("STUDENT", "DEACTIVATED")), "10 days"); // never counts

    MockHttpServletResponse response = call(BASE + "/summary", admin);

    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<Integer>read(body, "$.value")).isEqualTo(2);
    assertThat(JsonPath.<Double>read(body, "$.deltaPercent")).isEqualTo(100.0);
    assertThat(JsonPath.<String>read(body, "$.deltaDirection")).isEqualTo("up");
    List<Integer> spark = JsonPath.read(body, "$.sparklineSeries");
    assertThat(spark).hasSize(7);
    assertThat(spark.get(6)).isEqualTo(2); // the 30 days that end today
    assertThat(spark.get(0)).isZero(); // six days ago nobody had signed in yet
  }

  @Test
  void aZeroPreviousPeriodGivesNoPercentInsteadOfDividingByZero() throws Exception {
    String admin = login(account("ADMIN", "ACTIVE"));

    String body = call(BASE + "/summary", admin).getContentAsString();

    assertThat(JsonPath.<Integer>read(body, "$.value")).isEqualTo(1);
    assertThat(JsonPath.<Object>read(body, "$.deltaPercent")).isNull();
    assertThat(JsonPath.<String>read(body, "$.deltaDirection")).isEqualTo("none");
  }

  @Test
  void retentionHasSixMonthsOldestFirstWithReturningUsersOnlyFromEarlierAccounts()
      throws Exception {
    String admin = login(account("ADMIN", "ACTIVE"));
    String veteran = account("STUDENT", "ACTIVE");
    jdbc.update(
        "UPDATE identity.users SET created_at = now() - interval '70 days' WHERE email = ?",
        veteran);
    token(userId(veteran), "1 hour");

    String body = call(BASE + "/user-growth", admin).getContentAsString();

    List<String> months = JsonPath.read(body, "$.groups[*].month");
    YearMonth current = YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    assertThat(months).hasSize(6);
    assertThat(months.get(5)).isEqualTo(current.toString());
    assertThat(months.get(0)).isEqualTo(current.minusMonths(5).toString());
    assertThat(JsonPath.<Integer>read(body, "$.groups[5].returningUsers"))
        .isGreaterThanOrEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.groups[5].newUsers")).isGreaterThanOrEqualTo(1);
  }

  @Test
  void passwordResetsCountTheLast24HoursAgainstTheDayBeforeFromTheAuditLog() throws Exception {
    String adminEmail = account("ADMIN", "ACTIVE");
    String admin = login(adminEmail);
    UUID actor = userId(adminEmail);
    audit(actor, "1 hour");
    audit(actor, "2 hours");
    audit(actor, "30 hours");
    audit(actor, "10 days");

    String body = call(BASE + "/password-resets", admin).getContentAsString();

    assertThat(JsonPath.<Integer>read(body, "$.value")).isEqualTo(2);
    assertThat(JsonPath.<Double>read(body, "$.deltaPercent")).isEqualTo(100.0);
    assertThat(JsonPath.<String>read(body, "$.deltaDirection")).isEqualTo("up");
    List<Integer> spark = JsonPath.read(body, "$.sparklineSeries");
    assertThat(spark).hasSize(7);
    // The 30-hour row is inside the seven days, the ten-day row is not.
    assertThat(spark.stream().mapToInt(Integer::intValue).sum()).isEqualTo(3);
  }

  private void audit(UUID actor, String ago) {
    jdbc.update(
        "INSERT INTO identity.system_audit_logs (id, actor_user_id, action_type, target_type,"
            + " created_at) VALUES (?, ?, 'PASSWORD_RESET_REQUESTED', 'USER',"
            + " now() - CAST(? AS interval))",
        UUID.randomUUID(),
        actor,
        ago);
  }

  private void token(UUID user, String ago) {
    jdbc.update(
        "INSERT INTO identity.refresh_tokens (id, user_id, token_hash, family_id, issued_at,"
            + " expires_at) VALUES (?, ?, ?, ?, now() - CAST(? AS interval), now() + interval"
            + " '30 days')",
        UUID.randomUUID(),
        user,
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        ago);
  }

  private UUID userId(String email) {
    return jdbc.queryForObject("SELECT id FROM identity.users WHERE email = ?", UUID.class, email);
  }

  private String account(String roleCode, String status) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "deactivation_source,deactivated_at,must_change_password) SELECT ?,?,?,'QAUser123',"
            + "id,?,CASE WHEN ? = 'DEACTIVATED' THEN 'ADMIN' END,"
            + "CASE WHEN ? = 'DEACTIVATED' THEN now() END,false FROM identity.roles WHERE code = ?",
        UUID.randomUUID(),
        email,
        HASH,
        status,
        status,
        status,
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
