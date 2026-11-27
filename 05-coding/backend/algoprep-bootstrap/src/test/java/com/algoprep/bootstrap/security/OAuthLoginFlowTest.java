package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AvatarImageFetchPort;
import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.auth.OAuthProfile;
import com.algoprep.identity.domain.model.auth.OAuthProvider;
import com.jayway.jsonpath.JsonPath;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
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
import org.testcontainers.containers.wait.strategy.Wait;
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

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "oauth-test-user")
          .withEnv("MINIO_ROOT_PASSWORD", "oauth-test-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  private static String minioUrl() {
    return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
  }

  @BeforeAll
  static void createBucket() throws Exception {
    MinioClient.builder()
        .endpoint(minioUrl())
        .credentials("oauth-test-user", "oauth-test-password")
        .build()
        .makeBucket(MakeBucketArgs.builder().bucket("algoprep-avatars").build());
  }

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("algoprep.identity.avatar.storage.endpoint", OAuthLoginFlowTest::minioUrl);
    properties.add("algoprep.identity.avatar.storage.access-key", () -> "oauth-test-user");
    properties.add("algoprep.identity.avatar.storage.secret-key", () -> "oauth-test-password");
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @MockitoBean OAuthProviderPort provider;
  @MockitoBean AvatarImageFetchPort fetcher;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    reset(provider, fetcher);
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

  // ---- BR-28 avatar import (AC-103, AC-104)

  private static final String PICTURE = "https://avatars.githubusercontent.com/u/1?v=4";

  private static byte[] png() {
    byte[] d = new byte[40];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, d, 0, 8);
    d[12] = 'I';
    d[13] = 'H';
    d[14] = 'D';
    d[15] = 'R';
    d[19] = 8;
    d[23] = 8;
    return d;
  }

  private void scriptPicture(String providerUserId, String email, String picture) {
    when(provider.fetchProfile(any(), anyString()))
        .thenReturn(
            new OAuthProfile(
                OAuthProvider.GITHUB, providerUserId, email, true, "Nguyen An", picture));
  }

  private Integer avatarRows(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.user_avatars a JOIN identity.users u ON u.id = a.user_id"
            + " WHERE lower(u.email) = lower(?)",
        Integer.class,
        email);
  }

  private long avatarTime(String email) {
    return jdbc.queryForObject(
            "SELECT a.updated_at FROM identity.user_avatars a JOIN identity.users u ON u.id ="
                + " a.user_id WHERE u.email = ?",
            java.sql.Timestamp.class,
            email)
        .getTime();
  }

  private void awaitAvatar(String email) throws Exception {
    for (int i = 0; i < 100 && avatarRows(email) == 0; i++) {
      Thread.sleep(100);
    }
  }

  @Test
  void aFirstLinkImportsThePictureAndALaterLoginDoesNotFetchAgain() throws Exception {
    String email = email();
    scriptPicture("gh-" + UUID.randomUUID(), email, PICTURE);
    when(fetcher.fetch(anyString())).thenReturn(Optional.of(png()));

    assertThat(callback(startState(), "c1").getStatus()).isEqualTo(200);
    awaitAvatar(email);

    assertThat(avatarRows(email)).isEqualTo(1);
    verify(fetcher).fetch(PICTURE + "&s=256");
    long version = avatarTime(email);
    assertThat(callback(startState(), "c2").getStatus()).isEqualTo(200);
    Thread.sleep(300);
    verify(fetcher, times(1)).fetch(anyString());
    assertThat(avatarTime(email)).isEqualTo(version);
  }

  @Test
  void anAccountThatAlreadyHasAnAvatarKeepsItOnFirstLink() throws Exception {
    String email = email();
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,password_hash,display_name,role_id,status,must_change_password) SELECT ?,?,'h','Existing',id,'ACTIVE',false FROM identity.roles WHERE code='STUDENT'",
        id,
        email);
    jdbc.update(
        "INSERT INTO identity.user_avatars(user_id,object_key,content_type,size_bytes,updated_at)"
            + " VALUES (?, 'users/own/own.png', 'image/png', 10, now())",
        id);
    scriptPicture("gh-" + UUID.randomUUID(), email, PICTURE);
    when(fetcher.fetch(anyString())).thenReturn(Optional.of(png()));

    assertThat(callback(startState(), "c").getStatus()).isEqualTo(200);
    Thread.sleep(500);

    verify(fetcher, never()).fetch(anyString());
    assertThat(
            jdbc.queryForObject(
                "SELECT object_key FROM identity.user_avatars WHERE user_id = ?", String.class, id))
        .isEqualTo("users/own/own.png");
  }

  @Test
  void everyImportFailureStillLogsTheUserInAndStoresNothing() throws Exception {
    java.util.List<java.util.function.Supplier<Optional<byte[]>>> failures =
        java.util.List.of(
            Optional::empty,
            () -> Optional.of("<html>not an image</html>".getBytes()),
            () -> Optional.of(new byte[524_289]),
            () -> {
              throw new IllegalStateException("boom");
            });
    for (var failure : failures) {
      reset(fetcher);
      String email = email();
      scriptPicture("gh-" + UUID.randomUUID(), email, PICTURE);
      when(fetcher.fetch(anyString())).thenAnswer(call -> failure.get());

      var response = callback(startState(), "c");
      Thread.sleep(400);

      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(users(email)).isEqualTo(1);
      assertThat(avatarRows(email)).isZero();
    }
  }

  @Test
  void aLoginWithoutAPictureNeverCallsTheFetcher() throws Exception {
    String email = email();
    scriptPicture("gh-" + UUID.randomUUID(), email, null);

    assertThat(callback(startState(), "c").getStatus()).isEqualTo(200);
    Thread.sleep(200);

    verify(fetcher, never()).fetch(anyString());
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
        .thenReturn(
            new OAuthProfile(OAuthProvider.GITHUB, providerUserId, email, verified, name, null));
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
