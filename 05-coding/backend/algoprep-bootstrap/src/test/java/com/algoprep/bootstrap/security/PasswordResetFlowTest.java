package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.dto.ResetPasswordCommand;
import com.algoprep.identity.application.ports.in.ResetPasswordUseCase;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordResetOtpPort;
import com.algoprep.identity.domain.model.PasswordResetChallenge;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

/** BR-04: real PostgreSQL, Redis, SMTP and transaction/cleanup fault injection. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class PasswordResetFlowTest {
  private static final String BASE = "/api/v1/identity/auth";
  private static final String PASSWORD = "ValidPass123";
  private static final String NEW_PASSWORD = "ResetPass456";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);

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
  static final GenericContainer<?> MAIL =
      new GenericContainer<>("axllent/mailpit:v1.31.5").withExposedPorts(1025, 8025);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.mail.host", MAIL::getHost);
    properties.add("spring.mail.port", () -> MAIL.getMappedPort(1025));
  }

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private StringRedisTemplate redis;
  @Autowired private ResetPasswordUseCase resetUseCase;
  @MockitoSpyBean private EmailPort mail;
  @MockitoSpyBean private PasswordResetOtpPort otpStore;
  @MockitoSpyBean private AuditLogPort audit;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void forgotUnknownAndCooldownHaveSameResponseWithoutAdditionalSideEffects() throws Exception {
    String email = account("ACTIVE", false, HASH);
    Instant before = dbNow();
    MockHttpServletResponse first = forgot(email.toUpperCase());
    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(forgot(email).getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(forgot(email()).getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(first.getHeader("Set-Cookie")).isNull();
    assertThat(messages(email)).hasSize(1);
    assertThat(redis.getExpire(key(email))).isBetween(580L, 600L);
    assertThat(redis.getExpire(guard(email))).isBetween(40L, 60L);
    Instant issued = issued(email);
    assertThat(issued).isBetween(before, dbNow());
    assertThat(redis.opsForHash().get(key(email), "digest")).isNotEqualTo(code(email));
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isEqualTo(1);
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isZero();
    assertThat(user(email)).containsEntry("password_hash", HASH);
    assertAuditHasNoSecrets(email);
  }

  @Test
  void verifyRetainsOriginalCodeTtlPasswordAndSessions() throws Exception {
    String email = active();
    String cookie = loginCookie(email);
    forgot(email);
    String code = code(email);
    Instant issued = issued(email);
    redis.expire(key(email), Duration.ofSeconds(100));
    MockHttpServletResponse verified = verify(email, code);
    assertThat(verified.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(verified.getContentAsString(), "$.verified")).isTrue();
    assertThat(verified.getHeader("Set-Cookie")).isNull();
    assertThat(redis.getExpire(key(email))).isBetween(90L, 100L);
    assertThat(issued(email)).isEqualTo(issued);
    assertThat(user(email)).containsEntry("password_hash", HASH);
    assertThat(refresh(cookie).getStatus()).isEqualTo(200);
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isZero();
  }

  @Test
  void resetChangesPasswordRevokesAllRefreshSessionsAndCannotBeReplayed() throws Exception {
    String email = active();
    String first = loginCookie(email);
    String second = loginCookie(email);
    forgot(email);
    String code = code(email);
    Instant issued = issued(email);
    MockHttpServletResponse result = resetPassword(email.toUpperCase(), code, NEW_PASSWORD);
    assertThat(result.getStatus()).isEqualTo(200);
    assertThat(result.getHeader("Set-Cookie")).isNull();
    assertThat(result.getContentAsString())
        .contains("Đặt lại mật khẩu thành công.")
        .doesNotContain("accessToken", "refresh_token", "otp", "password");
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(redis.hasKey(guard(email))).isFalse();
    assertThat(((Timestamp) user(email).get("password_changed_at")).toInstant())
        .isAfterOrEqualTo(issued);
    assertThat(activeTokens(email)).isZero();
    assertThat(refresh(first).getStatus()).isEqualTo(401);
    assertThat(refresh(second).getStatus()).isEqualTo(401);
    assertError(login(email, PASSWORD), 401, "IDT-102");
    assertThat(login(email, NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertError(resetPassword(email, code, "OtherPass789"), 422, "IDT-107");
    assertError(verify(email, code), 422, "IDT-107");
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isEqualTo(1);
    assertAuditHasNoSecrets(email);
  }

  @Test
  void fifthFailureAcrossVerifyAndResetDeletesCodeAndPreservesCooldown() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    String wrong = wrong(code);
    redis.expire(key(email), Duration.ofSeconds(100));
    for (int i = 0; i < 4; i++) {
      assertError(verify(email, wrong), 422, "IDT-107");
    }
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("4");
    assertThat(redis.getExpire(key(email))).isBetween(90L, 100L);
    assertError(resetPassword(email, wrong, NEW_PASSWORD), 422, "IDT-107");
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(redis.hasKey(guard(email))).isTrue();
    assertError(resetPassword(email, code, NEW_PASSWORD), 422, "IDT-107");
    assertThat(user(email)).containsEntry("password_hash", HASH);
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isZero();
  }

  @Test
  void expiredUnknownMissingAndMalformedCodesShareGenericError() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    redis.expire(key(email), Duration.ofMillis(1));
    Thread.sleep(20);
    MockHttpServletResponse expired = resetPassword(email, code, NEW_PASSWORD);
    assertError(expired, 422, "IDT-107");
    assertThat(resetPassword(email(), code, NEW_PASSWORD).getContentAsString())
        .isEqualTo(expired.getContentAsString());
    assertError(verify(email, code), 422, "IDT-107");
    assertError(verify(email, "abc123"), 422, "IDT-107");
    assertError(verify(email, "12345"), 422, "IDT-107");
    assertThat(user(email)).containsEntry("password_hash", HASH);
  }

  @Test
  void resendAfterCooldownReplacesIssuanceAndResetsFailures() throws Exception {
    String email = active();
    forgot(email);
    Instant first = issued(email);
    assertError(verify(email, wrong(code(email))), 422, "IDT-107");
    redis.expire(key(email), Duration.ofSeconds(100));
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertThat(issued(email)).isEqualTo(first);
    assertThat(redis.getExpire(key(email))).isBetween(90L, 100L);
    redis.expire(guard(email), Duration.ofMillis(1));
    Thread.sleep(20);
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertThat(issued(email)).isAfter(first);
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("0");
    assertThat(redis.getExpire(key(email))).isBetween(590L, 600L);
    assertThat(messages(email)).hasSize(2);
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isEqualTo(2);
    assertThat(resetPassword(email, code(email), NEW_PASSWORD).getStatus()).isEqualTo(200);
  }

  @Test
  void wrongGuessesCountAgainstTheAccountAcrossFreshCodes() throws Exception {
    String email = active();
    for (int round = 0; round < 2; round++) {
      forgot(email);
      for (int attempt = 0; attempt < 5; attempt++) {
        assertError(verify(email, wrong(code(email))), 422, "IDT-107");
      }
      redis.expire(guard(email), Duration.ofMillis(1));
      Thread.sleep(20);
    }
    // Two codes each burned five guesses; asking for a third must not hand out a fresh budget.
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertError(verify(email, code(email)), 422, "IDT-107");
  }

  @Test
  void concurrentResetsAllowOnlyOnePasswordChange() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> first = () -> resetPassword(email, code, "ResetFirst123").getStatus();
      Callable<Integer> second = () -> resetPassword(email, code, "ResetSecond123").getStatus();
      var jobs = executor.invokeAll(List.of(first, second));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).containsExactlyInAnyOrder(200, 422);
    }
    assertThat(
            List.of(
                login(email, "ResetFirst123").getStatus(),
                login(email, "ResetSecond123").getStatus()))
        .containsExactlyInAnyOrder(200, 401);
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isEqualTo(1);
    assertThat(redis.hasKey(key(email))).isFalse();
  }

  @Test
  void concurrentForgotRequestsIssueOnlyOneCodeMailAndAudit() throws Exception {
    String email = active();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> request = () -> forgot(email).getStatus();
      var jobs = executor.invokeAll(List.of(request, request, request));
      for (var job : jobs) {
        assertThat(job.get()).isEqualTo(200);
      }
    }
    assertThat(messages(email)).hasSize(1);
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isEqualTo(1);
  }

  @Test
  void oauthOnlyGetsGuidanceButNoOtpOrResetAuditAndCannotResetEvenWithForgedStorage()
      throws Exception {
    String email = account("ACTIVE", false, null);
    MockHttpServletResponse first = forgot(email);
    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(forgot(email()).getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(forgot(email).getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(messages(email)).hasSize(1);
    assertThat(mailText(email)).contains("Google", "GitHub");
    assertThat(Pattern.compile("\\b[0-9]{6}\\b").matcher(mailText(email)).find()).isFalse();
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isZero();
    assertError(verify(email, "123456"), 422, "IDT-107");
    assertError(resetPassword(email, "123456", NEW_PASSWORD), 422, "IDT-107");
    assertThat(user(email).get("password_hash")).isNull();
    assertThat(activeTokens(email)).isZero();
  }

  @Test
  void adminCreatedPendingAndActiveForcedChangeAccountsClearFlags() throws Exception {
    for (String status : List.of("PENDING_VERIFICATION", "ACTIVE")) {
      String email = account(status, true, HASH);
      forgot(email);
      assertThat(resetPassword(email, code(email), NEW_PASSWORD).getStatus()).isEqualTo(200);
      assertThat(user(email))
          .containsEntry("status", "ACTIVE")
          .containsEntry("must_change_password", false);
      assertThat(login(email, NEW_PASSWORD).getStatus()).isEqualTo(200);
    }
  }

  @Test
  void selfRegisteredPendingMustUseEmailVerificationInsteadOfPasswordReset() throws Exception {
    String email = account("PENDING_VERIFICATION", false, HASH);
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertThat(messages(email)).isEmpty();
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isZero();
    assertError(resetPassword(email, "123456", NEW_PASSWORD), 422, "IDT-107");
    assertThat(user(email)).containsEntry("status", "PENDING_VERIFICATION");
  }

  @Test
  void resettingDeactivatedAccountDoesNotRestoreIt() throws Exception {
    String email = account("DEACTIVATED", false, HASH);
    jdbc.update(
        "UPDATE identity.users SET deactivation_source='ADMIN', deactivated_at=now() WHERE email=?",
        email);
    forgot(email);
    assertThat(resetPassword(email, code(email), NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertThat(user(email))
        .containsEntry("status", "DEACTIVATED")
        .containsEntry("deactivation_source", "ADMIN");
    assertError(login(email, NEW_PASSWORD), 403, "IDT-104");
    assertThat(activeTokens(email)).isZero();
  }

  @Test
  void passwordValidationKeepsCodeAndRejectsWeakOrOver72BytePasswords() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    assertError(resetPassword(email, code, "A1short"), 422, "IDT-V01");
    assertError(resetPassword(email, code, "é".repeat(35) + "A12"), 422, "IDT-V02");
    assertError(resetPassword(email, code, "abcdefgh"), 422, "IDT-V03");
    assertError(resetPassword(email, code, "QAUser123"), 422, "IDT-V04");
    assertError(resetPassword(email, code, email), 422, "IDT-V04");
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("0");
    assertThat(user(email)).containsEntry("password_hash", HASH);
    String accepted = "é".repeat(35) + "A1";
    assertThat(resetPassword(email, code, accepted).getStatus()).isEqualTo(200);
    assertThat(login(email, accepted).getStatus()).isEqualTo(200);
  }

  @Test
  void auditFailureRollsBackPasswordSessionRevocationsAndAuditWhileKeepingCode() throws Exception {
    String email = active();
    loginCookie(email);
    forgot(email);
    String code = code(email);
    Instant issued = issued(email);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("Injected audit failure after write");
            })
        .when(audit)
        .record(argThat(entry -> entry.actionType().equals("PASSWORD_RESET_COMPLETED")));
    assertThatThrownBy(
            () -> resetUseCase.reset(new ResetPasswordCommand(email, code, NEW_PASSWORD)))
        .hasRootCauseInstanceOf(IllegalStateException.class);
    assertThat(user(email))
        .containsEntry("password_hash", HASH)
        .containsEntry("password_changed_at", null);
    assertThat(activeTokens(email)).isEqualTo(1);
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isZero();
    assertThat(issued(email)).isEqualTo(issued);
    reset(audit);
    assertThat(resetPassword(email, code, NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertThat(activeTokens(email)).isZero();
  }

  @Test
  void failedRedisCleanupAfterCommitCannotAuthorizeAnotherReset() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    doThrow(new IllegalStateException("Injected cleanup failure"))
        .when(otpStore)
        .clear(any(), any());
    assertThat(resetPassword(email, code, NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertThat(redis.hasKey(key(email))).isTrue();
    assertError(verify(email, code), 422, "IDT-107");
    assertError(resetPassword(email, code, "OtherPass789"), 422, "IDT-107");
    assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isEqualTo(1);
    assertThat(login(email, NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertThat(redis.getExpire(key(email))).isBetween(570L, 600L);
  }

  @Test
  void smtpFailureRollsBackRequestedAuditAndRemovesIssuanceForRetry() throws Exception {
    String email = active();
    doThrow(new MailSendException("Injected SMTP failure"))
        .when(mail)
        .send(anyString(), anyString(), anyString());
    // Same 200 as for an unknown address, so a mail outage cannot be used to find real accounts.
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isZero();
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(redis.hasKey(guard(email))).isFalse();
    assertThat(user(email)).containsEntry("password_hash", HASH);
    reset(mail);
    assertThat(forgot(email).getStatus()).isEqualTo(200);
    assertThat(messages(email)).hasSize(1);
    assertThat(audits(email, "PASSWORD_RESET_REQUESTED")).isEqualTo(1);
  }

  @Test
  void redisOutageCannotVerifyOrResetAndRecoveryRetainsOriginalCode() throws Exception {
    String email = active();
    forgot(email);
    String code = code(email);
    REDIS.execInContainer("redis-cli", "CLIENT", "PAUSE", "2500", "ALL");
    try {
      assertThatThrownBy(() -> verify(email, code)).isInstanceOf(Exception.class);
      assertThatThrownBy(
              () -> resetUseCase.reset(new ResetPasswordCommand(email, code, NEW_PASSWORD)))
          .isInstanceOf(RuntimeException.class);
      assertThat(user(email)).containsEntry("password_hash", HASH);
      assertThat(audits(email, "PASSWORD_RESET_COMPLETED")).isZero();
    } finally {
      assertThat(REDIS.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }
    assertThat(resetPassword(email, code, NEW_PASSWORD).getStatus()).isEqualTo(200);
  }

  @Test
  void delayedCleanupPreservesNewIssuanceAndConcurrentFailuresDeleteAtLimit() throws Exception {
    String email = active();
    forgot(email);
    Instant old = issued(email);
    UUID id = (UUID) user(email).get("id");
    redis.delete(guard(email));
    Instant newer = old.plusSeconds(1);
    assertThat(otpStore.issue(id, new PasswordResetChallenge("test-digest", newer))).isTrue();
    otpStore.clear(id, old);
    assertThat(otpStore.verify(id, "test-digest")).contains(newer);
    assertThat(redis.opsForValue().get(guard(email))).isEqualTo(newer.toString());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var jobs =
          executor.invokeAll(
              IntStream.range(0, 12)
                  .<Callable<Boolean>>mapToObj(
                      i -> () -> otpStore.verify(id, "wrong-digest").isPresent())
                  .toList());
      for (var job : jobs) {
        assertThat(job.get()).isFalse();
      }
    }
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(otpStore.verify(id, "test-digest")).isEmpty();
  }

  private String active() {
    return account("ACTIVE", false, HASH);
  }

  private String account(String status, boolean mustChange, String hash) {
    String email = email();
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,must_change_password)"
            + " SELECT ?,?,?,'QAUser123',id,?,? FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email,
        hash,
        status,
        mustChange);
    return email;
  }

  private Map<String, Object> user(String email) {
    return jdbc.queryForMap("SELECT * FROM identity.users WHERE email=?", email);
  }

  private int audits(String email, String action) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type=?",
        Integer.class,
        user(email).get("id"),
        action);
  }

  private void assertAuditHasNoSecrets(String email) {
    assertThat(
            jdbc.queryForList(
                "SELECT before_json, after_json FROM identity.system_audit_logs WHERE actor_user_id=?"
                    + " AND action_type IN ('PASSWORD_RESET_REQUESTED', 'PASSWORD_RESET_COMPLETED')",
                user(email).get("id")))
        .allSatisfy(row -> assertThat(row.values()).containsOnlyNulls());
  }

  private int activeTokens(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.refresh_tokens WHERE user_id=? AND revoked_at IS NULL",
        Integer.class,
        user(email).get("id"));
  }

  private Instant dbNow() {
    return jdbc.queryForObject("SELECT clock_timestamp()", Timestamp.class).toInstant();
  }

  private Instant issued(String email) {
    return Instant.parse((String) redis.opsForHash().get(key(email), "issued_at"));
  }

  private String key(String email) {
    return "identity:pwreset:" + user(email).get("id");
  }

  private String guard(String email) {
    return "identity:pwreset:resend:" + user(email).get("id");
  }

  private static String email() {
    return UUID.randomUUID() + "@example.test";
  }

  private static String wrong(String code) {
    return code.equals("000000") ? "111111" : "000000";
  }

  private MockHttpServletResponse forgot(String email) throws Exception {
    return postJson("/password/forgot", "{\"email\":\"" + email + "\"}");
  }

  private MockHttpServletResponse verify(String email, String code) throws Exception {
    return postJson(
        "/password/verify-otp", "{\"email\":\"" + email + "\",\"otp\":\"" + code + "\"}");
  }

  private MockHttpServletResponse resetPassword(String email, String code, String password)
      throws Exception {
    return postJson(
        "/password/reset",
        "{\"email\":\""
            + email
            + "\",\"otp\":\""
            + code
            + "\",\"newPassword\":\""
            + password
            + "\"}");
  }

  private MockHttpServletResponse login(String email, String password) throws Exception {
    return postJson("/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
  }

  private String loginCookie(String email) throws Exception {
    MockHttpServletResponse response = login(email, PASSWORD);
    assertThat(response.getStatus()).isEqualTo(200);
    String cookie = response.getHeader("Set-Cookie");
    return cookie.substring("refresh_token=".length(), cookie.indexOf(';'));
  }

  private MockHttpServletResponse refresh(String token) throws Exception {
    return mvc.perform(
            post(BASE + "/refresh")
                .header("X-Requested-With", "XMLHttpRequest")
                .cookie(new Cookie("refresh_token", token)))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse postJson(String route, String body) throws Exception {
    return mvc.perform(post(BASE + route).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn()
        .getResponse();
  }

  private static void assertError(MockHttpServletResponse response, int status, String code)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode")).isEqualTo(code);
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  private List<Map<String, Object>> messages(String email) throws Exception {
    List<Map<String, Object>> messages = JsonPath.read(mailApi("/api/v1/messages"), "$.messages");
    return messages.stream()
        .filter(message -> message.get("To").toString().contains(email))
        .toList();
  }

  private String mailText(String email) throws Exception {
    return JsonPath.read(
        mailApi("/api/v1/message/" + messages(email).getFirst().get("ID")), "$.Text");
  }

  private String code(String email) throws Exception {
    String text = mailText(email);
    assertThat(text).doesNotContain(PASSWORD, NEW_PASSWORD);
    var match = Pattern.compile("\\b[0-9]{6}\\b").matcher(text);
    assertThat(match.find()).isTrue();
    return match.group();
  }

  private String mailApi(String path) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client
          .send(
              HttpRequest.newBuilder(
                      URI.create(
                          "http://" + MAIL.getHost() + ":" + MAIL.getMappedPort(8025) + path))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString())
          .body();
    }
  }
}
