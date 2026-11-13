package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * OAuth #5 and #6 over HTTP with real PostgreSQL and Redis; only the provider is scripted (its own
 * dialogue is covered by {@code HttpOAuthProviderAdapterTest}).
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
class OAuthLoginFlowTest {
  private static final String BASE = "/api/v1/identity/auth/oauth/github";

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

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @MockitoBean OAuthProviderPort provider;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    reset(provider);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(provider.authorizationUrl(any(), anyString()))
        .thenAnswer(call -> Optional.of("https://idp.test/auth?state=" + call.getArgument(1)));
  }

  @Test
  void redirectGivesAUrlWithAOneTimeStateKeptTenMinutes() throws Exception {
    var response = mvc.perform(get(BASE + "/redirect")).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    String state = JsonPath.read(response.getContentAsString(), "$.state");
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.authorizationUrl"))
        .endsWith(state);
    Long ttl = redis.getExpire("identity:oauthstate:" + state);
    assertThat(ttl).isBetween(1L, 600L);
  }

  @Test
  void aNewVerifiedUserIsCreatedWithoutAPasswordAndGetsASession() throws Exception {
    String email = email();
    scriptProfile("gh-" + UUID.randomUUID(), email, true, "Nguyen An");

    var response = callback(startState(), "code");

    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.accessToken")).isNotBlank();
    assertThat(response.getHeader("Set-Cookie"))
        .contains("refresh_token=", "HttpOnly", "SameSite=Strict")
        .contains("Path=/api/v1/identity/auth");
    var row =
        jdbc.queryForMap(
            "SELECT status, password_hash, display_name FROM identity.users WHERE email = ?",
            email);
    assertThat(row.get("status")).isEqualTo("ACTIVE");
    assertThat(row.get("password_hash")).isNull();
    assertThat(row.get("display_name")).isEqualTo("Nguyen An");
    assertThat(identities(email)).isEqualTo(1);
  }

  @Test
  void theSameProviderAccountSignsInAgainWithoutADuplicate() throws Exception {
    String email = email();
    scriptProfile("gh-fixed-" + email, email, true, "Nguyen An");

    assertThat(callback(startState(), "c1").getStatus()).isEqualTo(200);
    assertThat(callback(startState(), "c2").getStatus()).isEqualTo(200);

    assertThat(users(email)).isEqualTo(1);
    assertThat(identities(email)).isEqualTo(1);
  }

  @Test
  void aStateWorksOnceAndAnUnknownOneIs401() throws Exception {
    scriptProfile("gh-" + UUID.randomUUID(), email(), true, "Nguyen An");
    String state = startState();
    assertThat(callback(state, "c").getStatus()).isEqualTo(200);

    var replay = callback(state, "c");
    assertThat(replay.getStatus()).isEqualTo(401);
    assertThat(JsonPath.<String>read(replay.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-109");
    assertThat(callback("never-issued", "c").getStatus()).isEqualTo(401);
  }

  @Test
  void aRejectedCodeIs401AndAProviderOutageIs503() throws Exception {
    when(provider.fetchProfile(any(), anyString())).thenThrow(new OAuthCodeRejectedException());
    assertThat(callback(startState(), "bad").getStatus()).isEqualTo(401);

    reset(provider);
    when(provider.authorizationUrl(any(), anyString())).thenReturn(Optional.of("https://idp.test"));
    when(provider.fetchProfile(any(), anyString())).thenThrow(new IllegalStateException("down"));
    var down = callback(startState(), "c");
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(JsonPath.<String>read(down.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-505");
  }

  @Test
  void anUnverifiedProviderEmailIs422AndCreatesNothing() throws Exception {
    String email = email();
    scriptProfile("gh-" + UUID.randomUUID(), email, false, "Nguyen An");

    var response = callback(startState(), "c");

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-110");
    assertThat(users(email)).isZero();
    assertThat(identities(email)).isZero();
  }

  @Test
  void aVerifiedEmailOfAnExistingAccountIsLinkedAndItsPasswordIsKept() throws Exception {
    String email = email();
    jdbc.update(
        "INSERT INTO identity.users(id,email,password_hash,display_name,role_id,status,must_change_password) SELECT ?,?,'hash-stays','Existing',id,'ACTIVE',false FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email);
    scriptProfile("gh-" + UUID.randomUUID(), email, true, "Provider Name");

    assertThat(callback(startState(), "c").getStatus()).isEqualTo(200);

    assertThat(users(email)).isEqualTo(1);
    assertThat(identities(email)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM identity.users WHERE email = ?", String.class, email))
        .isEqualTo("hash-stays");
  }

  @Test
  void aPendingSelfRegisteredAccountIsClaimedAndLosesItsUnverifiedPassword() throws Exception {
    String email = email();
    jdbc.update(
        "INSERT INTO identity.users(id,email,password_hash,display_name,role_id,status,must_change_password) SELECT ?,?,'attacker-hash','Squatter',id,'PENDING_VERIFICATION',false FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email);
    scriptProfile("gh-" + UUID.randomUUID(), email, true, "Real Owner");

    assertThat(callback(startState(), "c").getStatus()).isEqualTo(200);

    var row =
        jdbc.queryForMap("SELECT status, password_hash FROM identity.users WHERE email = ?", email);
    assertThat(row.get("status")).isEqualTo("ACTIVE");
    assertThat(row.get("password_hash")).isNull();
  }

  @Test
  void aSelfDeactivatedAccountGetsOnlyARestoreTicketAndNoSession() throws Exception {
    String email = email();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status,deactivation_source,deactivated_at) SELECT ?,?,'Gone',id,'DEACTIVATED','SELF',now() FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email);
    scriptProfile("gh-" + UUID.randomUUID(), email, true, "Gone");

    var response = callback(startState(), "c");

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-104");
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.details.restoreTicket"))
        .isNotBlank();
    assertThat(response.getHeader("Set-Cookie")).isNull();
    assertThat(response.getContentAsString()).doesNotContain("accessToken");
  }

  @Test
  void anUnknownProviderIs400AndAnUnconfiguredOneIs503() throws Exception {
    var unknown =
        mvc.perform(get("/api/v1/identity/auth/oauth/facebook/redirect")).andReturn().getResponse();
    assertThat(unknown.getStatus()).isEqualTo(400);
    assertThat(JsonPath.<String>read(unknown.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V28");

    when(provider.authorizationUrl(any(), anyString())).thenReturn(Optional.empty());
    var off = mvc.perform(get(BASE + "/redirect")).andReturn().getResponse();
    assertThat(off.getStatus()).isEqualTo(503);
    assertThat(JsonPath.<String>read(off.getContentAsString(), "$.errorCode")).isEqualTo("IDT-505");
  }

  // ---- helpers

  private String startState() throws Exception {
    var response = mvc.perform(get(BASE + "/redirect")).andReturn().getResponse();
    return JsonPath.read(response.getContentAsString(), "$.state");
  }

  private MockHttpServletResponse callback(String state, String code) throws Exception {
    return mvc.perform(get(BASE + "/callback").param("code", code).param("state", state))
        .andReturn()
        .getResponse();
  }

  private void scriptProfile(String providerUserId, String email, boolean verified, String name) {
    when(provider.fetchProfile(any(), anyString()))
        .thenReturn(new OAuthProfile(OAuthProvider.GITHUB, providerUserId, email, verified, name));
  }

  private int users(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.users WHERE lower(email) = lower(?)", Integer.class, email);
  }

  private int identities(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.oauth_identities o JOIN identity.users u ON u.id = o.user_id WHERE lower(u.email) = lower(?)",
        Integer.class,
        email);
  }

  private static String email() {
    return "oauth-" + UUID.randomUUID() + "@example.test";
  }
}
