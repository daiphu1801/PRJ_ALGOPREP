package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.in.AnonymizeDeletedAccountsUseCase;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
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
 * #13 (BR-06) and #9 against real PostgreSQL and Redis: forced-change flag, session keep and
 * revoke.
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
class MyAccountFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String NEW_PASSWORD = "BrandNew456";
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
  @Autowired private AnonymizeDeletedAccountsUseCase anonymizeUseCase;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void changeKeepsCallerSessionRevokesOthersAndClearsTheForcedChangeFlag() throws Exception {
    String email = adminCreatedAccount();
    MockHttpServletResponse first = login(email, PASSWORD);
    assertThat(JsonPath.<Boolean>read(first.getContentAsString(), "$.user.mustChangePassword"))
        .isTrue();
    String callerAccess = JsonPath.read(first.getContentAsString(), "$.accessToken");
    String callerRefresh = refreshCookie(first);
    String otherRefresh = refreshCookie(login(email, PASSWORD));

    MockHttpServletResponse changed = change(callerAccess, PASSWORD, NEW_PASSWORD);

    assertThat(changed.getStatus()).isEqualTo(200);
    assertThat(refresh(callerRefresh).getStatus()).isEqualTo(200);
    assertThat(refresh(otherRefresh).getStatus()).isEqualTo(401);
    assertThat(login(email, PASSWORD).getStatus()).isEqualTo(401);
    MockHttpServletResponse again = login(email, NEW_PASSWORD);
    assertThat(again.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(again.getContentAsString(), "$.user.mustChangePassword"))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs a JOIN identity.users u"
                    + " ON u.id = a.actor_user_id WHERE u.email = ? AND a.action_type ="
                    + " 'PASSWORD_CHANGED'",
                Integer.class,
                email))
        .isEqualTo(1);
  }

  @Test
  void wrongCurrentPasswordIs422Idt115AndChangesNothing() throws Exception {
    String email = adminCreatedAccount();
    MockHttpServletResponse session = login(email, PASSWORD);
    String access = JsonPath.read(session.getContentAsString(), "$.accessToken");
    String otherRefresh = refreshCookie(login(email, PASSWORD));

    MockHttpServletResponse response = change(access, "NotThePass1", NEW_PASSWORD);

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-115");
    assertThat(refresh(otherRefresh).getStatus()).isEqualTo(200);
    assertThat(login(email, PASSWORD).getStatus()).isEqualTo(200);
  }

  @Test
  void oauthOnlyAccountSetsFirstPasswordThenNeedsTheCurrentOneToChangeAgain() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);
    String otherRefresh = refreshCookie(login(email, PASSWORD));
    // Turn the account into an OAuth-only one: no password hash at all.
    jdbc.update("UPDATE identity.users SET password_hash = NULL WHERE email = ?", email);

    // The "currentPassword" field is empty and must not matter.
    MockHttpServletResponse first = change(access, "", NEW_PASSWORD);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat((Boolean) JsonPath.read(first.getContentAsString(), "$.noticeEmailFailed"))
        .isFalse();
    assertThat(login(email, NEW_PASSWORD).getStatus()).isEqualTo(200);
    assertThat(refresh(otherRefresh).getStatus()).isEqualTo(401);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs a JOIN identity.users u"
                    + " ON u.id = a.actor_user_id WHERE u.email = ? AND a.action_type ="
                    + " 'PASSWORD_CHANGED'",
                Integer.class,
                email))
        .isEqualTo(1);
    List<Map<String, Object>> messages = JsonPath.read(mailApi("/api/v1/messages"), "$.messages");
    assertThat(
            messages.stream()
                .anyMatch(
                    m ->
                        m.get("To").toString().contains(email)
                            && m.get("Subject").toString().contains("đặt mật khẩu")))
        .isTrue();

    // Second call: the account now has a password, so the empty and wrong values are refused.
    assertThat(change(access, "", "Another789x").getStatus()).isEqualTo(422);
    assertThat(change(access, "NotThePass1", "Another789x").getStatus()).isEqualTo(422);
    assertThat(change(access, NEW_PASSWORD, "Another789x").getStatus()).isEqualTo(200);
  }

  @Test
  void oauthOnlyAccountCannotSetAWeakFirstPassword() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);
    jdbc.update("UPDATE identity.users SET password_hash = NULL WHERE email = ?", email);

    MockHttpServletResponse response = change(access, "", "short1");

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V01");
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash IS NULL FROM identity.users WHERE email = ?",
                Boolean.class,
                email))
        .isTrue();
  }

  @Test
  void profileReportsTheForcedChangeFlagAndLinkedProvidersWithoutSecrets() throws Exception {
    String email = adminCreatedAccount();
    jdbc.update(
        "INSERT INTO identity.oauth_identities (id,user_id,provider,provider_user_id,provider_email)"
            + " SELECT ?,id,'GOOGLE','google-subject-1','p@example.test' FROM identity.users"
            + " WHERE email=?",
        UUID.randomUUID(),
        email);
    String access = JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");

    MockHttpServletResponse before = profile(access);

    assertThat(before.getStatus()).isEqualTo(200);
    String body = before.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.email")).isEqualTo(email);
    assertThat(JsonPath.<String>read(body, "$.displayName")).isEqualTo("QAUser123");
    assertThat(JsonPath.<String>read(body, "$.roleName")).isEqualTo("Học viên");
    assertThat(JsonPath.<String>read(body, "$.defaultLanguage")).isEqualTo("PYTHON");
    assertThat(JsonPath.<Boolean>read(body, "$.hasPassword")).isTrue();
    assertThat(JsonPath.<Boolean>read(body, "$.mustChangePassword")).isTrue();
    assertThat(JsonPath.<Object>read(body, "$.passwordChangedAt")).isNull();
    assertThat(JsonPath.<List<String>>read(body, "$.linkedProviders")).containsExactly("GOOGLE");
    assertThat(JsonPath.<String>read(body, "$.joinedAt")).endsWith("Z");
    assertThat(body).doesNotContain("$2a$", "google-subject-1", "p@example.test", "passwordHash");

    assertThat(change(access, PASSWORD, NEW_PASSWORD).getStatus()).isEqualTo(200);
    String after = profile(access).getContentAsString();
    assertThat(JsonPath.<Boolean>read(after, "$.mustChangePassword")).isFalse();
    assertThat(JsonPath.<String>read(after, "$.passwordChangedAt")).endsWith("Z");
  }

  @Test
  void patchEditsOnlyTheSentFieldsAndIgnoresEmailRoleAndStatus() throws Exception {
    String email = adminCreatedAccount();
    String access = JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");

    MockHttpServletResponse first =
        patchProfile(
            access,
            "{\"displayName\":\"  New Name  \",\"schoolOrCompany\":\"HCMUT\","
                + "\"currentPosition\":\"Dev\",\"targetPosition\":\"Lead\","
                + "\"phone\":\"+84 90 123\",\"email\":\"hacker@example.test\","
                + "\"status\":\"DEACTIVATED\",\"role\":\"ADMIN\"}");

    assertThat(first.getStatus()).isEqualTo(200);
    String body = first.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.displayName")).isEqualTo("New Name");
    assertThat(JsonPath.<String>read(body, "$.schoolOrCompany")).isEqualTo("HCMUT");
    assertThat(JsonPath.<String>read(body, "$.phone")).isEqualTo("+84 90 123");
    assertThat(JsonPath.<String>read(body, "$.email")).isEqualTo(email);
    assertThat(JsonPath.<String>read(body, "$.roleName")).isEqualTo("Học viên");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM identity.users WHERE email = ?", String.class, email))
        .isEqualTo("ACTIVE");

    MockHttpServletResponse second =
        patchProfile(access, "{\"currentPosition\":\"\",\"displayName\":\"Other Name\"}");

    assertThat(second.getStatus()).isEqualTo(200);
    String again = second.getContentAsString();
    assertThat(JsonPath.<String>read(again, "$.displayName")).isEqualTo("Other Name");
    assertThat(JsonPath.<Object>read(again, "$.currentPosition")).isNull();
    assertThat(JsonPath.<String>read(again, "$.schoolOrCompany")).isEqualTo("HCMUT");
    assertThat(JsonPath.<String>read(again, "$.targetPosition")).isEqualTo("Lead");
  }

  @Test
  void patchRejectsInvalidValuesWith422AndKeepsTheStoredProfile() throws Exception {
    String email = adminCreatedAccount();
    String access = JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");

    MockHttpServletResponse response =
        patchProfile(
            access,
            "{\"displayName\":\"Fine Name\",\"schoolOrCompany\":\"" + "x".repeat(151) + "\"}");

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V09");
    assertThat(JsonPath.<String>read(profile(access).getContentAsString(), "$.displayName"))
        .isEqualTo("QAUser123");
  }

  @Test
  void patchWithoutBearerTokenIs401() throws Exception {
    assertThat(
            mvc.perform(
                    patch("/api/v1/identity/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void emailChangeSendsACodeToTheNewAddressAndOnlyTheRightCodeMovesTheLogin() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);
    String newEmail = UUID.randomUUID() + "@example.test";

    MockHttpServletResponse asked = emailRequest(access, newEmail);

    assertThat(asked.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(asked.getContentAsString(), "$.pendingEmail"))
        .isEqualTo(newEmail);
    assertThat(JsonPath.<Integer>read(asked.getContentAsString(), "$.expiresInSeconds"))
        .isEqualTo(600);
    assertThat(storedEmail(email)).isEqualTo(email);
    assertThat(emailRequest(access, newEmail).getStatus()).isEqualTo(429);

    String code = mailedCode(newEmail);
    String wrong = "000000".equals(code) ? "111111" : "000000";
    MockHttpServletResponse bad = emailConfirm(access, wrong);
    assertThat(bad.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(bad.getContentAsString(), "$.errorCode")).isEqualTo("IDT-416");
    assertThat(storedEmail(email)).isEqualTo(email);

    MockHttpServletResponse done = emailConfirm(access, code);

    assertThat(done.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(done.getContentAsString(), "$.email")).isEqualTo(newEmail);
    assertThat(login(newEmail, PASSWORD).getStatus()).isEqualTo(200);
    assertThat(login(email, PASSWORD).getStatus()).isEqualTo(401);
    assertThat(emailConfirm(access, code).getStatus()).isEqualTo(422);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs a JOIN identity.users u"
                    + " ON u.id = a.actor_user_id WHERE u.email = ? AND a.action_type ="
                    + " 'EMAIL_CHANGED'",
                Integer.class,
                newEmail))
        .isEqualTo(1);
  }

  @Test
  void emailChangeRefusesTheCurrentAndAnAlreadyUsedAddressWithoutSendingMail() throws Exception {
    String email = adminCreatedAccount();
    String other = adminCreatedAccount();
    String access = loginAccess(email);

    for (String address : List.of(email, other)) {
      MockHttpServletResponse response = emailRequest(access, address);
      assertThat(response.getStatus()).isEqualTo(409);
      assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-415");
    }

    assertThat(mailsTo(other)).isZero();
    assertThat(emailRequest(access, "not-an-email").getStatus()).isEqualTo(400);
  }

  @Test
  void emailChangeAllowsThreeRequestsPerDayEvenWhenTheCooldownIsOver() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);
    UUID userId =
        jdbc.queryForObject("SELECT id FROM identity.users WHERE email = ?", UUID.class, email);
    String newEmail = UUID.randomUUID() + "@example.test";

    for (int i = 0; i < 3; i++) {
      assertThat(emailRequest(access, newEmail).getStatus()).isEqualTo(200);
      redis.delete("identity:emailchange:resend:" + userId);
    }

    MockHttpServletResponse fourth = emailRequest(access, newEmail);
    assertThat(fourth.getStatus()).isEqualTo(429);
    assertThat(JsonPath.<String>read(fourth.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V05");
  }

  @Test
  void emailChangeWithoutBearerTokenIs401() throws Exception {
    assertThat(
            mvc.perform(
                    post("/api/v1/identity/me/email-change/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"a@example.test\"}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void settingsStartAtTheDefaultsAndPatchCreatesThePreferencesRow() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);

    String defaults = settings(access).getContentAsString();
    assertThat(JsonPath.<Boolean>read(defaults, "$.streakReminderEnabled")).isTrue();
    assertThat(JsonPath.<Boolean>read(defaults, "$.weeklyReportEnabled")).isFalse();
    assertThat(JsonPath.<String>read(defaults, "$.preferredInterviewLevel")).isEqualTo("MIDDLE");
    assertThat(JsonPath.<Integer>read(defaults, "$.preferredMaxTurns")).isEqualTo(12);
    assertThat(JsonPath.<Boolean>read(defaults, "$.preferredHintAllowed")).isTrue();
    assertThat(JsonPath.<String>read(defaults, "$.defaultLanguage")).isEqualTo("PYTHON");
    assertThat(preferenceRows(email)).isZero();

    MockHttpServletResponse saved =
        patchSettings(
            access,
            "{\"weeklyReportEnabled\":true,\"preferredMaxTurns\":16,\"defaultLanguage\":\"JAVA\"}");

    assertThat(saved.getStatus()).isEqualTo(200);
    assertThat(preferenceRows(email)).isEqualTo(1);
    String after = settings(access).getContentAsString();
    assertThat(JsonPath.<Boolean>read(after, "$.weeklyReportEnabled")).isTrue();
    assertThat(JsonPath.<Integer>read(after, "$.preferredMaxTurns")).isEqualTo(16);
    assertThat(JsonPath.<String>read(after, "$.defaultLanguage")).isEqualTo("JAVA");
    assertThat(JsonPath.<Boolean>read(after, "$.streakReminderEnabled")).isTrue();
    assertThat(JsonPath.<String>read(after, "$.preferredInterviewLevel")).isEqualTo("MIDDLE");
    assertThat(
            jdbc.queryForObject(
                "SELECT default_language FROM identity.users WHERE email = ?", String.class, email))
        .isEqualTo("JAVA");

    patchSettings(access, "{\"preferredHintAllowed\":false}");
    assertThat(
            JsonPath.<Boolean>read(settings(access).getContentAsString(), "$.preferredHintAllowed"))
        .isFalse();
    assertThat(preferenceRows(email)).isEqualTo(1);
  }

  @Test
  void settingsRejectValuesOutsideTheAllowedSetWithoutChangingAnything() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);

    MockHttpServletResponse badTurns = patchSettings(access, "{\"preferredMaxTurns\":10}");
    MockHttpServletResponse badLanguage =
        patchSettings(access, "{\"weeklyReportEnabled\":true,\"defaultLanguage\":\"RUST\"}");

    assertThat(badTurns.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(badTurns.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V18");
    assertThat(badLanguage.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(badLanguage.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-413");
    assertThat(preferenceRows(email)).isZero();
    assertThat(
            JsonPath.<Boolean>read(settings(access).getContentAsString(), "$.weeklyReportEnabled"))
        .isFalse();
  }

  @Test
  void twoSettingsPatchesAtOnceBothKeepTheirValues() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      for (int round = 0; round < 20; round++) {
        String language = round % 2 == 0 ? "JAVA" : "CPP";
        String level = round % 2 == 0 ? "SENIOR" : "JUNIOR";
        var first =
            pool.submit(() -> patchSettings(access, "{\"defaultLanguage\":\"" + language + "\"}"));
        var second =
            pool.submit(
                () -> patchSettings(access, "{\"preferredInterviewLevel\":\"" + level + "\"}"));
        assertThat(first.get().getStatus()).isEqualTo(200);
        assertThat(second.get().getStatus()).isEqualTo(200);

        String stored = settings(access).getContentAsString();
        assertThat(JsonPath.<String>read(stored, "$.defaultLanguage")).isEqualTo(language);
        assertThat(JsonPath.<String>read(stored, "$.preferredInterviewLevel")).isEqualTo(level);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void settingsWithoutBearerTokenAre401() throws Exception {
    assertThat(
            mvc.perform(get("/api/v1/identity/me/settings")).andReturn().getResponse().getStatus())
        .isEqualTo(401);
  }

  @Test
  void selfDeletionNeedsTheOwnEmailThenBlocksTheSessionAndStartsTheGracePeriod() throws Exception {
    String email = adminCreatedAccount();
    MockHttpServletResponse session = login(email, PASSWORD);
    String access = JsonPath.read(session.getContentAsString(), "$.accessToken");
    String refresh = refreshCookie(session);

    MockHttpServletResponse wrong = deleteAccount(access, "someone-else@example.test");

    assertThat(wrong.getStatus()).isEqualTo(422);
    assertThat(JsonPath.<String>read(wrong.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-201");
    assertThat(userStatus(email)).isEqualTo("ACTIVE");

    assertThat(deleteAccount(access, " " + email.toUpperCase() + " ").getStatus()).isEqualTo(200);

    assertThat(userStatus(email)).isEqualTo("DEACTIVATED");
    assertThat(
            jdbc.queryForObject(
                "SELECT deactivation_source FROM identity.users WHERE email = ?",
                String.class,
                email))
        .isEqualTo("SELF");
    assertThat(profile(access).getStatus()).isEqualTo(401);
    assertThat(refresh(refresh).getStatus()).isEqualTo(401);
    MockHttpServletResponse again = login(email, PASSWORD);
    assertThat(again.getStatus()).isEqualTo(403);
    assertThat(JsonPath.<String>read(again.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-104");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs a JOIN identity.users u"
                    + " ON u.id = a.actor_user_id WHERE u.email = ? AND a.action_type ="
                    + " 'SELF_ACCOUNT_DELETE_REQUESTED'",
                Integer.class,
                email))
        .isEqualTo(1);
  }

  @Test
  void anonymisationOnlyTouchesSelfDeletedAccountsPastTheGracePeriod() throws Exception {
    String due = deactivated("SELF", 31);
    String recent = deactivated("SELF", 5);
    String lockedByAdmin = deactivated("ADMIN", 400);
    UUID dueId = userId(due);
    jdbc.update(
        "INSERT INTO identity.oauth_identities (id,user_id,provider,provider_user_id,provider_email)"
            + " VALUES (?,?,'GITHUB','gh-1','real@provider.test')",
        UUID.randomUUID(),
        dueId);

    assertThat(anonymizeUseCase.anonymize()).isEqualTo(1);

    assertThat(
            jdbc.queryForObject(
                "SELECT email FROM identity.users WHERE id = ?", String.class, dueId))
        .isEqualTo("deleted-user-" + dueId + "@anon.local");
    assertThat(
            jdbc.queryForMap(
                "SELECT display_name, phone, school_or_company, anonymized_at IS NOT NULL AS done"
                    + " FROM identity.users WHERE id = ?",
                dueId))
        .containsEntry("display_name", "Người dùng đã xoá")
        .containsEntry("phone", null)
        .containsEntry("done", true);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.oauth_identities WHERE user_id = ?",
                Integer.class,
                dueId))
        .isZero();
    assertThat(storedEmail(recent)).isEqualTo(recent);
    assertThat(storedEmail(lockedByAdmin)).isEqualTo(lockedByAdmin);
    assertThat(anonymizeUseCase.anonymize()).isZero();
  }

  @Test
  void profileExportReturnsOwnDataWithoutSecretsAndAllowsOneEveryFiveMinutes() throws Exception {
    String email = adminCreatedAccount();
    String access = loginAccess(email);

    MockHttpServletResponse first = export(access, "profile");

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(first.getHeader("Content-Disposition")).contains("algoprep-profile.json");
    String body = first.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.profile.email")).isEqualTo(email);
    assertThat(JsonPath.<String>read(body, "$.settings.defaultLanguage")).isEqualTo("PYTHON");
    assertThat(body).doesNotContain("$2a$", "passwordHash", "password_hash");

    MockHttpServletResponse again = export(access, "profile");
    assertThat(again.getStatus()).isEqualTo(429);
    assertThat(JsonPath.<String>read(again.getContentAsString(), "$.errorCode"))
        .isEqualTo("IDT-V06");
  }

  @Test
  void anUnknownExportScopeIs400AndTheSubmissionFileOfAnEmptyHistoryIsHeaderOnly()
      throws Exception {
    String access = loginAccess(adminCreatedAccount());

    assertThat(export(access, "everything").getStatus()).isEqualTo(400);
    MockHttpServletResponse submissions = export(access, "submissions");
    assertThat(submissions.getStatus()).isEqualTo(200);
    assertThat(submissions.getContentAsString().lines().count()).isEqualTo(1);
  }

  @Test
  void theSoleActiveAdminCannotDeleteItselfButOneWithAnotherActiveAdminCan() throws Exception {
    List<UUID> others =
        jdbc.queryForList(
            "SELECT u.id FROM identity.users u JOIN identity.roles r ON r.id = u.role_id"
                + " WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE'",
            UUID.class);
    for (UUID other : others) {
      jdbc.update("UPDATE identity.users SET status = 'PENDING_VERIFICATION' WHERE id = ?", other);
    }
    try {
      String sole = activeAdmin();
      String soleAccess = accessToken(sole);

      MockHttpServletResponse refused = deleteAccount(soleAccess, sole);

      assertThat(refused.getStatus()).isEqualTo(422);
      assertThat(JsonPath.<String>read(refused.getContentAsString(), "$.errorCode"))
          .isEqualTo("IDT-432");
      assertThat(userStatus(sole)).isEqualTo("ACTIVE");

      String second = activeAdmin();

      assertThat(deleteAccount(soleAccess, sole).getStatus()).isEqualTo(200);
      assertThat(userStatus(sole)).isEqualTo("DEACTIVATED");
      assertThat(userStatus(second)).isEqualTo("ACTIVE");
    } finally {
      for (UUID other : others) {
        jdbc.update("UPDATE identity.users SET status = 'ACTIVE' WHERE id = ?", other);
      }
    }
  }

  private String activeAdmin() {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'QAAdmin123',id,'ACTIVE',false"
            + " FROM identity.roles WHERE code='ADMIN'",
        UUID.randomUUID(),
        email,
        HASH);
    return email;
  }

  private String accessToken(String email) throws Exception {
    return JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");
  }

  @Test
  void exportWithoutBearerTokenIs401() throws Exception {
    assertThat(
            mvc.perform(get("/api/v1/identity/me/export?scope=profile"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void selfDeletionWithoutBearerTokenIs401() throws Exception {
    assertThat(
            mvc.perform(
                    post("/api/v1/identity/me/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailConfirmation\":\"a@example.test\"}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void profileWithoutBearerTokenIs401() throws Exception {
    assertThat(
            mvc.perform(get("/api/v1/identity/me/profile")).andReturn().getResponse().getStatus())
        .isEqualTo(401);
  }

  @Test
  void withoutBearerTokenIs401() throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post("/api/v1/identity/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPassword\":\"x\",\"newPassword\":\"y\"}"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
  }

  private String adminCreatedAccount() {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,'QAUser123',id,'PENDING_VERIFICATION',true"
            + " FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email,
        HASH);
    return email;
  }

  private MockHttpServletResponse login(String email, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse change(String access, String current, String next)
      throws Exception {
    return mvc.perform(
            post("/api/v1/identity/me/password")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"))
        .andReturn()
        .getResponse();
  }

  private String loginAccess(String email) throws Exception {
    return JsonPath.read(login(email, PASSWORD).getContentAsString(), "$.accessToken");
  }

  private String storedEmail(String email) {
    return jdbc.queryForObject(
        "SELECT email FROM identity.users WHERE lower(email) = lower(?)", String.class, email);
  }

  private MockHttpServletResponse emailRequest(String access, String newEmail) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/me/email-change/request")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newEmail\":\"" + newEmail + "\"}"))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse emailConfirm(String access, String code) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/me/email-change/confirm")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andReturn()
        .getResponse();
  }

  private int mailsTo(String address) throws Exception {
    List<Map<String, Object>> messages = JsonPath.read(mailApi("/api/v1/messages"), "$.messages");
    return (int) messages.stream().filter(m -> m.get("To").toString().contains(address)).count();
  }

  private String mailedCode(String address) throws Exception {
    List<Map<String, Object>> messages = JsonPath.read(mailApi("/api/v1/messages"), "$.messages");
    Object id =
        messages.stream()
            .filter(m -> m.get("To").toString().contains(address))
            .findFirst()
            .orElseThrow()
            .get("ID");
    String text = JsonPath.read(mailApi("/api/v1/message/" + id), "$.Text");
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
                  .build(),
              HttpResponse.BodyHandlers.ofString())
          .body();
    }
  }

  private MockHttpServletResponse deleteAccount(String access, String typedEmail) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/me/account/delete")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailConfirmation\":\"" + typedEmail + "\"}"))
        .andReturn()
        .getResponse();
  }

  private String userStatus(String email) {
    return jdbc.queryForObject(
        "SELECT status FROM identity.users WHERE email = ?", String.class, email);
  }

  private UUID userId(String email) {
    return jdbc.queryForObject("SELECT id FROM identity.users WHERE email = ?", UUID.class, email);
  }

  /** An account deactivated by {@code source} that many days ago. */
  private String deactivated(String source, int daysAgo) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "deactivation_source,deactivated_at,school_or_company,phone) SELECT ?,?,?,'QAUser123',"
            + "id,'DEACTIVATED',?,clock_timestamp() - make_interval(days => ?),'HCMUT','0123'"
            + " FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email,
        HASH,
        source,
        daysAgo);
    return email;
  }

  private MockHttpServletResponse export(String access, String scope) throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/export")
                .param("scope", scope)
                .header("Authorization", "Bearer " + access))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse settings(String access) throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/settings").header("Authorization", "Bearer " + access))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse patchSettings(String access, String json) throws Exception {
    return mvc.perform(
            patch("/api/v1/identity/me/settings")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse();
  }

  private int preferenceRows(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.user_preferences p JOIN identity.users u ON u.id ="
            + " p.user_id WHERE u.email = ?",
        Integer.class,
        email);
  }

  private MockHttpServletResponse profile(String access) throws Exception {
    return mvc.perform(
            get("/api/v1/identity/me/profile").header("Authorization", "Bearer " + access))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse patchProfile(String access, String json) throws Exception {
    return mvc.perform(
            patch("/api/v1/identity/me/profile")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse refresh(String token) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/auth/refresh")
                .header("X-Requested-With", "XMLHttpRequest")
                .cookie(new Cookie("refresh_token", token)))
        .andReturn()
        .getResponse();
  }

  private static String refreshCookie(MockHttpServletResponse response) {
    String cookie = response.getHeader("Set-Cookie");
    assertThat(cookie).isNotNull();
    return cookie.substring("refresh_token=".length(), cookie.indexOf(';'));
  }
}
