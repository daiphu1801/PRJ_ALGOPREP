package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Login, refresh rotation, reuse detection and logout against the real schema (DD api F1 section 1,
 * DD logic BR-02 and BR-21). Needs Docker.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
class AuthSessionFlowTest {

  private static final String PASSWORD = "Passw0rd!x";
  private static final String BASE = "/api/v1/identity/auth";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DisplayName("login returns token, user and a hardened cookie, and stores only the token hash")
  void loginIssuesSession() throws Exception {
    String email = account("INSTRUCTOR", "ACTIVE", false);

    MockHttpServletResponse res = login(email, PASSWORD);

    assertThat(res.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(res.getContentAsString(), "$.accessToken")).isNotBlank();
    assertThat(JsonPath.<Integer>read(res.getContentAsString(), "$.expiresIn")).isEqualTo(900);
    assertThat(JsonPath.<String>read(res.getContentAsString(), "$.user.baseCategory"))
        .isEqualTo("INSTRUCTOR");
    String setCookie = res.getHeader("Set-Cookie");
    assertThat(setCookie)
        .contains("refresh_token=", "HttpOnly", "SameSite=Strict", "Path=" + BASE, "Secure");
    String raw = cookieValue(res);
    assertThat(count("SELECT count(*) FROM identity.refresh_tokens WHERE token_hash = ?", raw))
        .isZero();
    assertThat(
            count("SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ?", userId(email)))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("wrong password and unknown email give the same 401 IDT-102")
  void badCredentialsAreIndistinguishable() throws Exception {
    String email = account("STUDENT", "ACTIVE", false);

    MockHttpServletResponse wrong = login(email, "nope-nope-1");
    MockHttpServletResponse unknown = login("ghost@test.local", PASSWORD);

    assertThat(wrong.getStatus()).isEqualTo(401);
    assertThat(unknown.getStatus()).isEqualTo(401);
    assertThat(wrong.getContentAsString()).contains("IDT-102");
    assertThat(unknown.getContentAsString()).contains("IDT-102");
  }

  @Test
  @DisplayName("status gates: deactivated 403 IDT-104, unverified self-register 403 IDT-113")
  void statusGates() throws Exception {
    MockHttpServletResponse deactivated = login(account("STUDENT", "DEACTIVATED", false), PASSWORD);
    String selfRegistered = account("STUDENT", "PENDING_VERIFICATION", false);
    MockHttpServletResponse unverified = login(selfRegistered, PASSWORD);

    assertThat(deactivated.getStatus()).isEqualTo(403);
    assertThat(deactivated.getContentAsString()).contains("IDT-104");
    assertThat(unverified.getStatus()).isEqualTo(403);
    assertThat(unverified.getContentAsString()).contains("IDT-113");
    assertThat(
            count(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ?",
                userId(selfRegistered)))
        .isZero();
  }

  @Test
  @DisplayName("admin-created account becomes ACTIVE on first login and keeps mustChangePassword")
  void adminCreatedAccountActivatesOnFirstLogin() throws Exception {
    String email = account("STUDENT", "PENDING_VERIFICATION", true);

    MockHttpServletResponse res = login(email, PASSWORD);

    assertThat(res.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(res.getContentAsString(), "$.user.mustChangePassword"))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM identity.users WHERE lower(email) = ?", String.class, email))
        .isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("instructor portal admits instructors only, 403 IDT-111 otherwise")
  void instructorPortalChecksRole() throws Exception {
    MockHttpServletResponse student =
        loginAt(BASE + "/instructor/login", account("STUDENT", "ACTIVE", false), PASSWORD);
    MockHttpServletResponse instructor =
        loginAt(BASE + "/instructor/login", account("INSTRUCTOR", "ACTIVE", false), PASSWORD);

    assertThat(student.getStatus()).isEqualTo(403);
    assertThat(student.getContentAsString()).contains("IDT-111");
    assertThat(instructor.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("refresh rotates the token; reusing the old one revokes the family and audits it")
  void refreshRotationAndReuseDetection() throws Exception {
    String email = account("STUDENT", "ACTIVE", false);
    String first = cookieValue(login(email, PASSWORD));

    MockHttpServletResponse rotated = refresh(first);
    String second = cookieValue(rotated);
    assertThat(rotated.getStatus()).isEqualTo(200);
    assertThat(second).isNotEqualTo(first);

    MockHttpServletResponse reused = refresh(first);
    assertThat(reused.getStatus()).isEqualTo(401);
    assertThat(reused.getContentAsString()).contains("IDT-106");
    assertThat(refresh(second).getContentAsString()).contains("IDT-106");
    // Each presentation of a revoked token is a reuse event (BR-02), so two audit rows by now.
    assertThat(
            count(
                "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id = ? AND"
                    + " action_type = 'SECURITY_TOKEN_REUSE_DETECTED'",
                userId(email)))
        .isEqualTo(2);
    assertThat(
            count(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ? AND revoked_at IS"
                    + " NULL",
                userId(email)))
        .isZero();
  }

  @Test
  @DisplayName("refresh rejects a missing header, a missing or unknown cookie, and expiry")
  void refreshRejectsBadInput() throws Exception {
    String email = account("STUDENT", "ACTIVE", false);
    String token = cookieValue(login(email, PASSWORD));

    MockHttpServletResponse noHeader =
        mvc.perform(post(BASE + "/refresh").cookie(new Cookie("refresh_token", token)))
            .andReturn()
            .getResponse();
    assertThat(noHeader.getStatus()).isEqualTo(400);
    assertThat(noHeader.getContentAsString()).contains("IDT-V11");

    assertThat(refresh(null).getContentAsString()).contains("IDT-105");
    assertThat(refresh("unknown-token").getContentAsString()).contains("IDT-105");

    jdbc.update(
        "UPDATE identity.refresh_tokens SET expires_at = now() - interval '1 second'"
            + " WHERE user_id = ?",
        userId(email));
    MockHttpServletResponse expired = refresh(token);
    assertThat(expired.getStatus()).isEqualTo(401);
    assertThat(expired.getContentAsString()).contains("IDT-105");
  }

  @Test
  @DisplayName("a deactivated user cannot refresh even with a live token")
  void deactivatedUserCannotRefresh() throws Exception {
    String email = account("STUDENT", "ACTIVE", false);
    String token = cookieValue(login(email, PASSWORD));
    jdbc.update(
        "UPDATE identity.users SET status = 'DEACTIVATED', deactivation_source = 'ADMIN',"
            + " deactivated_at = now() WHERE lower(email) = ?",
        email);

    MockHttpServletResponse res = refresh(token);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("IDT-105");
  }

  @Test
  @DisplayName("logout revokes the token, clears the cookie, and is idempotent")
  void logoutRevokes() throws Exception {
    String email = account("STUDENT", "ACTIVE", false);
    String token = cookieValue(login(email, PASSWORD));

    MockHttpServletResponse out =
        mvc.perform(post(BASE + "/logout").cookie(new Cookie("refresh_token", token)))
            .andReturn()
            .getResponse();
    assertThat(out.getStatus()).isEqualTo(204);
    assertThat(out.getHeader("Set-Cookie")).contains("refresh_token=", "Max-Age=0");
    assertThat(
            count(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ? AND revoked_at IS"
                    + " NULL",
                userId(email)))
        .isZero();

    assertThat(mvc.perform(post(BASE + "/logout")).andReturn().getResponse().getStatus())
        .isEqualTo(204);
    assertThat(refresh(token).getStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("the access token from login opens a layer-2 gate for an instructor")
  void accessTokenWorksOnProtectedEndpoint() throws Exception {
    String email = account("INSTRUCTOR", "ACTIVE", false);
    String access = JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");

    int status =
        mvc.perform(get("/probe/class-read").header("Authorization", "Bearer " + access))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isEqualTo(200);
  }

  private MockHttpServletResponse login(String email, String password) throws Exception {
    return loginAt(BASE + "/login", email, password);
  }

  private MockHttpServletResponse loginAt(String path, String email, String password)
      throws Exception {
    return mvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse refresh(String token) throws Exception {
    var request = post(BASE + "/refresh").header("X-Requested-With", "XMLHttpRequest");
    if (token != null) {
      request.cookie(new Cookie("refresh_token", token));
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static String cookieValue(MockHttpServletResponse response) {
    String header = response.getHeader("Set-Cookie");
    assertThat(header).isNotNull();
    return header.substring("refresh_token=".length(), header.indexOf(';'));
  }

  private String account(String roleCode, String status, boolean mustChangePassword) {
    UUID id = UUID.randomUUID();
    String email = id + "@test.local";
    String source = "DEACTIVATED".equals(status) ? "ADMIN" : null;
    jdbc.update(
        "INSERT INTO identity.users (id, email, password_hash, display_name, role_id, status,"
            + " must_change_password, deactivation_source) SELECT ?, ?, ?, ?, r.id, ?, ?, ? FROM"
            + " identity.roles r WHERE r.code = ?",
        id,
        email,
        PASSWORD_HASH,
        "User " + roleCode,
        status,
        mustChangePassword,
        source,
        roleCode);
    return email;
  }

  private UUID userId(String email) {
    return jdbc.queryForObject(
        "SELECT id FROM identity.users WHERE lower(email) = ?", UUID.class, email);
  }

  private int count(String sql, Object arg) {
    Integer value = jdbc.queryForObject(sql, Integer.class, arg);
    return value == null ? 0 : value;
  }
}
