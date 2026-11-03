package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.dto.VerifyEmailCommand;
import com.algoprep.identity.application.ports.in.VerifyEmailUseCase;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.EmailVerificationOtpPort;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
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

/** Endpoints #1/#53/#54: real DB/Redis/SMTP, with faults injected at outbound ports. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class RegistrationFlowTest {
  private static final String BASE = "/api/v1/identity/auth";
  private static final String PASSWORD = "ValidPass123";

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
  @Autowired private VerifyEmailUseCase verification;
  @MockitoSpyBean private EmailPort mail;
  @MockitoSpyBean private EmailVerificationOtpPort otpStore;
  @MockitoSpyBean private RegistrationRepository accounts;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void registrationSendsOtpAndActivationAllowsLoginWithoutIssuingSession() throws Exception {
    String email = email();
    MockHttpServletResponse result = register(email.toUpperCase(), PASSWORD, "  QA New  ");
    assertThat(result.getStatus()).isEqualTo(201);
    assertThat(JsonPath.<String>read(result.getContentAsString(), "$.email")).isEqualTo(email);
    assertThat(result.getHeader("Set-Cookie")).isNull();
    assertThat(result.getContentAsString()).doesNotContain("password", "accessToken", "otp");
    Map<String, Object> user = user(email);
    assertThat(user)
        .containsEntry("status", "PENDING_VERIFICATION")
        .containsEntry("must_change_password", false)
        .containsEntry("display_name", "QA New")
        .containsEntry("code", "STUDENT");
    assertThat(new BCryptPasswordEncoder().matches(PASSWORD, (String) user.get("password_hash")))
        .isTrue();
    String code = code(email);
    assertThat(redis.opsForHash().get(key(email), "digest")).isNotEqualTo(code);
    assertThat(redis.getExpire(key(email))).isBetween(580L, 600L);
    assertThat(redis.getExpire(guard(email))).isBetween(40L, 60L);
    assertError(postJson("/login", credentials(email, PASSWORD)), 403, "IDT-113");
    assertError(postJson("/instructor/login", credentials(email, PASSWORD)), 403, "IDT-113");
    MockHttpServletResponse verified = verify(email.toUpperCase(), code);
    assertThat(verified.getStatus()).isEqualTo(200);
    assertThat(verified.getHeader("Set-Cookie")).isNull();
    assertThat(user(email)).containsEntry("status", "ACTIVE");
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(verify(email, code).getStatus()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = ?",
                Integer.class,
                user.get("id")))
        .isZero();
    assertThat(postJson("/login", credentials(email, PASSWORD)).getStatus()).isEqualTo(200);
  }

  @Test
  void duplicateCaseVariantAndConcurrentRegistrationCreateOneAccountAndOneMail() throws Exception {
    String email = email();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> lower = () -> register(email, PASSWORD, "QA").getStatus();
      Callable<Integer> upper = () -> register(email.toUpperCase(), PASSWORD, "QA").getStatus();
      var jobs = executor.invokeAll(List.of(lower, upper));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).containsExactlyInAnyOrder(201, 409);
    }
    assertError(register(email.toUpperCase(), PASSWORD, "QA"), 409, "IDT-101");
    assertThat(messages(email)).hasSize(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.users WHERE lower(email)=?", Integer.class, email))
        .isEqualTo(1);
  }

  @Test
  void fiveWrongCodesExhaustBudgetWithoutExtendingTtl() throws Exception {
    String email = registered();
    String correct = code(email);
    String wrong = correct.equals("000000") ? "111111" : "000000";
    redis.expire(key(email), Duration.ofSeconds(100));
    for (int i = 0; i < 5; i++) {
      assertError(verify(email, wrong), 422, "IDT-112");
    }
    assertError(verify(email, correct), 422, "IDT-112");
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("5");
    assertThat(redis.getExpire(key(email))).isBetween(90L, 100L);
    assertThat(user(email)).containsEntry("status", "PENDING_VERIFICATION");
  }

  @Test
  void missingExpiredAndUnknownCodesHaveSameGenericError() throws Exception {
    String email = registered();
    String correct = code(email);
    redis.expire(key(email), Duration.ofMillis(1));
    Thread.sleep(20);
    MockHttpServletResponse expired = verify(email, correct);
    MockHttpServletResponse unknown = verify(email(), correct);
    assertError(expired, 422, "IDT-112");
    assertThat(unknown.getContentAsString()).isEqualTo(expired.getContentAsString());
    assertError(verify(email, "12345"), 422, "IDT-112");
  }

  @Test
  void resendEnforcesCooldownReplacesCodeAndPreservesGenericResponse() throws Exception {
    String email = registered();
    Object digest = redis.opsForHash().get(key(email), "digest");
    assertError(resend(email), 429, "IDT-V12");
    assertThat(messages(email)).hasSize(1);
    assertThat(redis.opsForHash().get(key(email), "digest")).isEqualTo(digest);
    redis.expire(guard(email), Duration.ofMillis(1));
    Thread.sleep(20);
    MockHttpServletResponse response = resend(email.toUpperCase());
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(messages(email)).hasSize(2);
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("0");
    assertThat(redis.getExpire(key(email))).isBetween(590L, 600L);
    assertThat(resend(email()).getContentAsString()).isEqualTo(response.getContentAsString());
    assertThat(verify(email, code(email)).getStatus()).isEqualTo(200);
    assertThat(resend(email).getContentAsString()).isEqualTo(response.getContentAsString());
    assertThat(messages(email)).hasSize(2);
  }

  @Test
  void adminCreatedAndDeactivatedAccountsCannotReceiveOrUseEmailOtp() throws Exception {
    for (String status : List.of("PENDING_VERIFICATION", "DEACTIVATED")) {
      String email = email();
      jdbc.update(
          "INSERT INTO identity.users (id,email,display_name,role_id,status,must_change_password)"
              + " SELECT ?,?,'Admin created',id,?,true FROM identity.roles WHERE code='STUDENT'",
          UUID.randomUUID(),
          email,
          status);
      assertThat(resend(email).getStatus()).isEqualTo(200);
      assertError(verify(email, "123456"), 422, "IDT-112");
      assertThat(redis.hasKey(key(email))).isFalse();
      assertThat(messages(email)).isEmpty();
      assertThat(user(email))
          .containsEntry("status", status)
          .containsEntry("must_change_password", true);
    }
  }

  @Test
  void concurrentWrongAttemptsCannotLoseCounterUpdates() throws Exception {
    String email = registered();
    String correct = code(email);
    String wrong = correct.equals("000000") ? "111111" : "000000";
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var jobs =
          executor.invokeAll(
              java.util.stream.IntStream.range(0, 12)
                  .<Callable<Integer>>mapToObj(i -> () -> verify(email, wrong).getStatus())
                  .toList());
      for (var job : jobs) {
        assertThat(job.get()).isEqualTo(422);
      }
    }
    assertThat(redis.opsForHash().get(key(email), "failures")).isEqualTo("5");
    assertError(verify(email, correct), 422, "IDT-112");
  }

  @Test
  void concurrentResendsSendOnlyOneNewMail() throws Exception {
    String email = registered();
    redis.delete(guard(email));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> request = () -> resend(email).getStatus();
      var jobs = executor.invokeAll(List.of(request, request));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).containsExactlyInAnyOrder(200, 429);
    }
    assertThat(messages(email)).hasSize(2);
  }

  @Test
  void delayedCleanupCannotDeleteANewerIssuance() throws Exception {
    String email = registered();
    UUID id = (UUID) user(email).get("id");
    String oldDigest = (String) redis.opsForHash().get(key(email), "digest");
    redis.delete(guard(email));
    assertThat(otpStore.issue(id, "new-test-digest")).isTrue();
    otpStore.clear(id, oldDigest);
    assertThat(redis.opsForHash().get(key(email), "digest")).isEqualTo("new-test-digest");
    assertThat(redis.opsForValue().get(guard(email))).isEqualTo("new-test-digest");
    assertThat(redis.getExpire(key(email))).isBetween(590L, 600L);
  }

  @Test
  void concurrentSuccessfulVerificationActivatesOnlyOnce() throws Exception {
    String email = registered();
    String code = code(email);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> request = () -> verify(email, code).getStatus();
      var jobs = executor.invokeAll(List.of(request, request));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).containsOnly(200);
    }
    assertThat(user(email)).containsEntry("status", "ACTIVE");
    org.mockito.Mockito.verify(accounts, org.mockito.Mockito.times(1))
        .activate((UUID) user(email).get("id"));
    assertThat(redis.hasKey(key(email))).isFalse();
  }

  @Test
  void smtpFailureRollsBackRegistrationAndClearsIssuance() throws Exception {
    String email = email();
    doThrow(new MailSendException("Injected SMTP failure"))
        .when(mail)
        .send(anyString(), anyString(), anyString());
    assertThatThrownBy(() -> register(email, PASSWORD, "QA"))
        .hasRootCauseInstanceOf(MailSendException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.users WHERE email=?", Integer.class, email))
        .isZero();
    reset(mail);
    assertThat(register(email, PASSWORD, "QA").getStatus()).isEqualTo(201);
    assertThat(messages(email)).hasSize(1);
  }

  @Test
  void failedActivationKeepsOtpAndCommittedActivationSurvivesCleanupFailure() throws Exception {
    String email = registered();
    String code = code(email);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("Injected DB failure");
            })
        .when(accounts)
        .activate(any());
    assertThatThrownBy(() -> verification.verify(new VerifyEmailCommand(email, code)))
        .hasRootCauseInstanceOf(IllegalStateException.class);
    assertThat(user(email)).containsEntry("status", "PENDING_VERIFICATION");
    assertThat(redis.hasKey(key(email))).isTrue();
    reset(accounts);
    doThrow(new IllegalStateException("Injected cleanup failure"))
        .when(otpStore)
        .clear(any(), anyString());
    assertThat(verify(email, code).getStatus()).isEqualTo(200);
    assertThat(user(email)).containsEntry("status", "ACTIVE");
    assertThat(redis.hasKey(key(email))).isTrue();
    assertThat(verify(email, code).getStatus()).isEqualTo(200);
    assertThat(redis.getExpire(key(email))).isBetween(580L, 600L);
  }

  @Test
  void redisOutageCannotCreateOrActivateAccount() throws Exception {
    String email = registered();
    String code = code(email);
    String fresh = email();
    REDIS.execInContainer("redis-cli", "CLIENT", "PAUSE", "2500", "ALL");
    try {
      assertThatThrownBy(() -> verification.verify(new VerifyEmailCommand(email, code)))
          .isInstanceOf(RuntimeException.class);
      assertThatThrownBy(() -> register(fresh, PASSWORD, "QA")).isInstanceOf(Exception.class);
      assertThat(user(email)).containsEntry("status", "PENDING_VERIFICATION");
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM identity.users WHERE email=?", Integer.class, fresh))
          .isZero();
    } finally {
      assertThat(REDIS.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }
    assertThat(verify(email, code).getStatus()).isEqualTo(200);
  }

  @Test
  void inputValidationRejectsWeakPasswordsAndBadNamesBeforePersistence() throws Exception {
    assertError(register(email(), "Abc123", "QA"), 422, "IDT-V01");
    assertError(register(email(), "A1".repeat(65), "QA"), 422, "IDT-V02");
    assertError(register(email(), "abcdefgh", "QA"), 422, "IDT-V03");
    assertError(register(email(), "12345678", "QA"), 422, "IDT-V03");
    assertError(register(email(), "ValidPass123", "ValidPass123"), 422, "IDT-V04");
    assertError(register(email(), PASSWORD, " x "), 422, "IDT-V19");
    assertError(register(email(), PASSWORD, "x".repeat(101)), 422, "IDT-V20");
    assertError(register("bad email", PASSWORD, "QA"), 400, "IDT-400");
    assertError(postJson("/register", "{}"), 400, "IDT-400");
  }

  @Test
  void asciiAndUnicodePasswordsRespect72ByteBoundaryThroughRegistrationAndLogin() throws Exception {
    for (String password : List.of("A".repeat(70) + "A1", "é".repeat(35) + "A1")) {
      String email = email();
      assertError(register(email, password + "x", "QA"), 422, "IDT-V02");
      assertThat(register(email, password, "QA").getStatus()).isEqualTo(201);
      assertThat(verify(email, code(email)).getStatus()).isEqualTo(200);
      assertThat(postJson("/login", credentials(email, password)).getStatus()).isEqualTo(200);
      assertError(postJson("/login", credentials(email, password + "x")), 401, "IDT-102");
    }
  }

  private String registered() throws Exception {
    String email = email();
    assertThat(register(email, PASSWORD, "QA").getStatus()).isEqualTo(201);
    return email;
  }

  private MockHttpServletResponse register(String email, String password, String name)
      throws Exception {
    return postJson(
        "/register",
        "{\"email\":\""
            + email
            + "\",\"password\":\""
            + password
            + "\",\"displayName\":\""
            + name
            + "\",\"role\":\"ADMIN\"}");
  }

  private MockHttpServletResponse verify(String email, String code) throws Exception {
    return postJson("/email/verify", "{\"email\":\"" + email + "\",\"otp\":\"" + code + "\"}");
  }

  private MockHttpServletResponse resend(String email) throws Exception {
    return postJson("/email/verify/resend", "{\"email\":\"" + email + "\"}");
  }

  private MockHttpServletResponse postJson(String route, String body) throws Exception {
    return mvc.perform(post(BASE + route).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn()
        .getResponse();
  }

  private static String credentials(String email, String password) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
  }

  private static void assertError(MockHttpServletResponse response, int status, String code)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.errorCode")).isEqualTo(code);
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  private Map<String, Object> user(String email) {
    return jdbc.queryForMap(
        "SELECT u.*, r.code FROM identity.users u JOIN identity.roles r ON r.id=u.role_id WHERE u.email=?",
        email);
  }

  private String key(String email) {
    return "identity:emailverify:" + user(email).get("id");
  }

  private String guard(String email) {
    return "identity:emailverify:resend:" + user(email).get("id");
  }

  private static String email() {
    return UUID.randomUUID() + "@example.test";
  }

  private List<Map<String, Object>> messages(String email) throws Exception {
    List<Map<String, Object>> messages = JsonPath.read(mailApi("/api/v1/messages"), "$.messages");
    return messages.stream()
        .filter(message -> message.get("To").toString().contains(email))
        .toList();
  }

  private String code(String email) throws Exception {
    List<Map<String, Object>> messages = messages(email);
    String text =
        JsonPath.read(mailApi("/api/v1/message/" + messages.getFirst().get("ID")), "$.Text");
    assertThat(text).doesNotContain(PASSWORD);
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
