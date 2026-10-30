package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * End-to-end check of the F1 security foundation against the real schema: JWT verification, live
 * account status, live permission matrix, and the 403 shape. Needs Docker.
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
class SecurityFoundationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired private WebApplicationContext context;
  @Autowired private AccessTokenPort tokens;
  @Autowired private JdbcTemplate jdbc;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DisplayName("no token is 401, a garbage token is 401")
  void missingOrBrokenTokenIsUnauthorized() throws Exception {
    assertThat(status(get("/probe/class-read"))).isEqualTo(401);
    assertThat(status(get("/probe/class-read").header("Authorization", "Bearer not-a-jwt")))
        .isEqualTo(401);
  }

  @Test
  @DisplayName("instructor passes a CLASS_MANAGEMENT:READ gate, student gets 403 IDT-445")
  void layerTwoGateFollowsTheMatrix() throws Exception {
    String instructor = tokenFor("INSTRUCTOR", "ACTIVE", BaseCategory.INSTRUCTOR);
    String student = tokenFor("STUDENT", "ACTIVE", BaseCategory.STUDENT);

    assertThat(status(authed("/probe/class-read", instructor))).isEqualTo(200);

    MvcResult denied = mvc.perform(authed("/probe/class-read", student)).andReturn();
    assertThat(denied.getResponse().getStatus()).isEqualTo(403);
    assertThat(denied.getResponse().getContentAsString()).contains("\"errorCode\":\"IDT-445\"");
  }

  @Test
  @DisplayName("layer one gate uses base category: only ADMIN passes hasRole(ADMIN)")
  void layerOneGateUsesBaseCategory() throws Exception {
    String admin = tokenFor("ADMIN", "ACTIVE", BaseCategory.ADMIN);
    String instructor = tokenFor("INSTRUCTOR", "ACTIVE", BaseCategory.INSTRUCTOR);

    assertThat(status(authed("/probe/admin-only", admin))).isEqualTo(200);
    assertThat(status(authed("/probe/admin-only", instructor))).isEqualTo(403);
  }

  @Test
  @DisplayName("a deactivated or pending account loses access with its still-valid token")
  void inactiveAccountIsRejectedEveryRequest() throws Exception {
    String token = tokenFor("INSTRUCTOR", "ACTIVE", BaseCategory.INSTRUCTOR);
    UUID userId = subjectOf(token);
    assertThat(status(authed("/probe/class-read", token))).isEqualTo(200);

    jdbc.update(
        "UPDATE identity.users SET status = 'DEACTIVATED', deactivation_source = 'SELF',"
            + " deactivated_at = now() WHERE id = ?",
        userId);

    assertThat(status(authed("/probe/class-read", token))).isEqualTo(401);
  }

  @Test
  @DisplayName("a matrix change applies to the very next request (permission_version)")
  void matrixChangeAppliesImmediately() throws Exception {
    String token = tokenFor("INSTRUCTOR", "ACTIVE", BaseCategory.INSTRUCTOR);
    assertThat(status(authed("/probe/class-read", token))).isEqualTo(200);

    setClassReadGranted(false);
    try {
      assertThat(status(authed("/probe/class-read", token))).isEqualTo(403);
    } finally {
      setClassReadGranted(true);
    }
    assertThat(status(authed("/probe/class-read", token))).isEqualTo(200);
  }

  @Test
  @DisplayName("public auth paths are not blocked by the filter chain")
  void authPathsArePublic() throws Exception {
    assertThat(status(get("/api/v1/identity/auth/anything"))).isNotEqualTo(401).isNotEqualTo(403);
  }

  private void setClassReadGranted(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = ? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE r.id = p.role_id AND f.id = p.function_id AND a.id ="
            + " p.action_id AND r.code = 'INSTRUCTOR' AND f.code = 'CLASS_MANAGEMENT' AND a.code ="
            + " 'READ'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version = permission_version + 1 WHERE code ="
            + " 'INSTRUCTOR'");
  }

  private String tokenFor(String roleCode, String status, BaseCategory category) {
    UUID userId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id, email, display_name, role_id, status)"
            + " SELECT ?, ?, ?, r.id, ? FROM identity.roles r WHERE r.code = ?",
        userId,
        userId + "@test.local",
        "User " + roleCode,
        status,
        roleCode);
    return tokens.issue(userId, category, roleCode).value();
  }

  private static UUID subjectOf(String jwt) throws Exception {
    String payload = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("\"sub\":\"([^\"]+)\"").matcher(payload);
    assertThat(m.find()).isTrue();
    return UUID.fromString(m.group(1));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authed(
      String path, String token) {
    return get(path).header("Authorization", "Bearer " + token);
  }

  private int status(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
    return mvc.perform(request).andReturn().getResponse().getStatus();
  }
}
