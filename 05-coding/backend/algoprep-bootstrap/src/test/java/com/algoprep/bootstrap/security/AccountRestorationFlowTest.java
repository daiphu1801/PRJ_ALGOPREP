package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.ports.in.RestoreDeactivatedAccountUseCase;
import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.model.RestorationTicketSecrets;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** BR-08/#61: password proof, one-use tickets, SQL locks and dependency failures. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class AccountRestorationFlowTest {
  private static final String BASE = "/api/v1/identity/auth";
  private static final String PASSWORD = "ValidPass123";
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

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private StringRedisTemplate redis;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private RestoreDeactivatedAccountUseCase restoration;
  @MockitoSpyBean private RestorationTicketPort tickets;
  @MockitoSpyBean private AccountRestorationRepository accounts;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void eligibleLoginReturnsOnlyTicketThenRestoreRequiresANewLogin() throws Exception {
    String email = deleted("SELF", 2, false);
    Map<String, Object> before = user(email);
    MockHttpServletResponse denied = login(email.toUpperCase(), PASSWORD, "/login");
    String ticket = ticket(denied);
    assertThat(denied.getHeader("Set-Cookie")).isNull();
    assertThat(denied.getContentAsString())
        .doesNotContain("accessToken", "password", "refresh_token");
    assertThat(ticket).matches("[A-Za-z0-9_-]{43}").doesNotContain(".");
    assertThat(redis.getExpire(key(ticket))).isBetween(280L, 300L);
    assertThat(redis.opsForValue().get(key(ticket))).doesNotContain(ticket);
    assertThat(user(email))
        .containsEntry("status", "DEACTIVATED")
        .containsEntry("last_active_at", null);
    assertThat(sessionCount(email)).isZero();
    MockHttpServletResponse restored = restore(ticket);
    assertThat(restored.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Boolean>read(restored.getContentAsString(), "$.restored")).isTrue();
    assertThat(restored.getHeader("Set-Cookie")).isNull();
    assertThat(restored.getContentAsString()).doesNotContain("accessToken", "refresh_token");
    assertThat(user(email))
        .containsEntry("status", "ACTIVE")
        .containsEntry("deactivation_source", null)
        .containsEntry("deactivated_at", null)
        .containsEntry("password_hash", before.get("password_hash"));
    assertThat(redis.hasKey(key(ticket))).isFalse();
    assertThat(sessionCount(email)).isZero();
    assertError(restore(ticket), 401, "IDT-114");
    MockHttpServletResponse loggedIn = login(email, PASSWORD, "/login");
    assertThat(loggedIn.getStatus()).isEqualTo(200);
    assertThat(loggedIn.getHeader("Set-Cookie")).contains("refresh_token=");
  }

  @Test
  void wrongCredentialsAndUnknownEmailNeverIssueTicket() throws Exception {
    String email = deleted("SELF", 1, false);
    int before = redis.keys("identity:restore:*").size();
    for (String route : List.of("/login", "/instructor/login")) {
      MockHttpServletResponse denied = login(email, "wrong", route);
      assertError(denied, 401, "IDT-102");
      assertThat(denied.getContentAsString()).doesNotContain("restoreTicket", "details");
    }
    assertError(login(UUID.randomUUID() + "@example.test", PASSWORD, "/login"), 401, "IDT-102");
    assertThat(redis.keys("identity:restore:*")).hasSize(before);
    assertThat(sessionCount(email)).isZero();
  }

  @Test
  void instructorAndOrdinaryLoginBothReturnEligibleTicket() throws Exception {
    String email = deleted("SELF", 1, false);
    jdbc.update(
        "UPDATE identity.users SET role_id=(SELECT id FROM identity.roles WHERE code='INSTRUCTOR') WHERE email=?",
        email);
    String first = ticket(login(email, PASSWORD, "/login"));
    String second = ticket(login(email, PASSWORD, "/instructor/login"));
    assertThat(first).isNotEqualTo(second);
    assertThat(sessionCount(email)).isZero();
    assertThat(restore(second).getStatus()).isEqualTo(200);
    assertError(restore(first), 401, "IDT-114");
  }

  @Test
  void adminLockedExpiredAt30DaysAndAnonymizedAccountsReceiveNoTicket() throws Exception {
    List<String> emails =
        List.of(
            deleted("ADMIN", 1, false),
            deleted("SELF", 30, false),
            deleted("SELF", 31, false),
            deleted("SELF", 1, true),
            deleted(null, 1, false));
    for (String email : emails) {
      MockHttpServletResponse denied = login(email, PASSWORD, "/login");
      assertError(denied, 403, "IDT-104");
      assertThat(denied.getContentAsString()).doesNotContain("restoreTicket", "details");
      assertThat(user(email)).containsEntry("status", "DEACTIVATED");
      assertThat(sessionCount(email)).isZero();
    }
  }

  @Test
  void changingSourceAnonymizingOrChangingDeletionAfterIssuanceInvalidatesTicket()
      throws Exception {
    for (String update :
        List.of(
            "deactivation_source='ADMIN'",
            "anonymized_at=clock_timestamp()",
            "deactivated_at=clock_timestamp() - interval '31 days'",
            "status='ACTIVE', deactivation_source=NULL, deactivated_at=NULL")) {
      String email = deleted("SELF", 1, false);
      String ticket = ticket(login(email, PASSWORD, "/login"));
      jdbc.update("UPDATE identity.users SET " + update + " WHERE email=?", email);
      Map<String, Object> before = user(email);
      assertError(restore(ticket), 401, "IDT-114");
      assertThat(user(email)).isEqualTo(before);
      assertThat(sessionCount(email)).isZero();
    }
  }

  @Test
  void expiredUnknownMalformedAndUsedTicketsHaveSameGenericError() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    redis.expire(key(ticket), Duration.ofMillis(1));
    Thread.sleep(20);
    MockHttpServletResponse expired = restore(ticket);
    assertError(expired, 401, "IDT-114");
    for (String value : List.of("", "invalid", "x.y.z", RestorationTicketSecrets.generate())) {
      MockHttpServletResponse result = restore(value);
      assertError(result, 401, "IDT-114");
      assertThat(result.getContentAsString()).isEqualTo(expired.getContentAsString());
    }
    assertError(postJson("/deactivation/restore", "{}"), 401, "IDT-114");
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
  }

  @Test
  void corruptedRedisGrantCannotRestoreAccount() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    for (String value : List.of("broken", "not-a-uuid|bad-time|bad-time")) {
      redis.opsForValue().set(key(ticket), value, Duration.ofMinutes(5));
      assertError(restore(ticket), 401, "IDT-114");
      assertThat(user(email)).containsEntry("status", "DEACTIVATED");
    }
  }

  @Test
  void concurrentRedemptionRestoresExactlyOnceWithoutSessions() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> request = () -> restore(ticket).getStatus();
      var jobs = executor.invokeAll(List.of(request, request));
      assertThat(List.of(jobs.get(0).get(), jobs.get(1).get())).containsExactlyInAnyOrder(200, 401);
    }
    assertThat(user(email)).containsEntry("status", "ACTIVE");
    assertThat(sessionCount(email)).isZero();
    org.mockito.Mockito.verify(accounts, org.mockito.Mockito.times(1)).restore(any());
  }

  @Test
  void unusedTicketFromEarlierDeletionCannotRestoreLaterDeletion() throws Exception {
    String email = deleted("SELF", 1, false);
    String first = ticket(login(email, PASSWORD, "/login"));
    String unused = ticket(login(email, PASSWORD, "/login"));
    assertThat(restore(first).getStatus()).isEqualTo(200);
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
            + " deactivated_at=clock_timestamp() WHERE email=?",
        email);
    assertError(restore(unused), 401, "IDT-114");
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
    String fresh = ticket(login(email, PASSWORD, "/login"));
    assertThat(restore(fresh).getStatus()).isEqualTo(200);
  }

  @Test
  void databaseFailureRollsBackRestoreAndRetainsTicketForRetry() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    Map<String, Object> before = user(email);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("Injected DB failure after restore UPDATE");
            })
        .when(accounts)
        .restore(any());
    assertThatThrownBy(() -> restoration.restore(new RestoreAccountCommand(ticket)))
        .hasRootCauseInstanceOf(IllegalStateException.class);
    assertThat(user(email)).isEqualTo(before);
    assertThat(redis.hasKey(key(ticket))).isTrue();
    assertThat(redis.getExpire(key(ticket))).isBetween(280L, 300L);
    reset(accounts);
    assertThat(restore(ticket).getStatus()).isEqualTo(200);
    assertThat(sessionCount(email)).isZero();
  }

  @Test
  void redisCleanupFailureAfterCommitCannotReplayEvenAfterAnotherDeletion() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    doThrow(new IllegalStateException("Injected cleanup failure"))
        .when(tickets)
        .delete(anyString());
    assertThat(restore(ticket).getStatus()).isEqualTo(200);
    assertThat(redis.hasKey(key(ticket))).isTrue();
    assertError(restore(ticket), 401, "IDT-114");
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
            + " deactivated_at=clock_timestamp() WHERE email=?",
        email);
    assertError(restore(ticket), 401, "IDT-114");
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
    assertThat(sessionCount(email)).isZero();
  }

  @Test
  void redisOutageCannotIssueOrRedeemTicket() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    REDIS.execInContainer("redis-cli", "CLIENT", "PAUSE", "2500", "ALL");
    try {
      assertThatThrownBy(() -> restore(ticket)).isInstanceOf(Exception.class);
      assertThatThrownBy(() -> login(email, PASSWORD, "/login")).isInstanceOf(Exception.class);
      assertThat(user(email)).containsEntry("status", "DEACTIVATED");
      assertThat(sessionCount(email)).isZero();
    } finally {
      assertThat(REDIS.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }
    assertThat(restore(ticket).getStatus()).isEqualTo(200);
  }

  @Test
  void ticketExpiryWhileWaitingForUserLockIsRejectedEvenIfRedisKeyRemains() throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    UUID id = (UUID) user(email).get("id");
    Instant expiry = dbNow().plusMillis(800);
    String value =
        id + "|" + ((Timestamp) user(email).get("deactivated_at")).toInstant() + "|" + expiry;
    redis.opsForValue().set(key(ticket), value, Duration.ofMinutes(5));
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch entered = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              entered.countDown();
              return invocation.callRealMethod();
            })
        .when(accounts)
        .lockById(id);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var locker =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          status -> {
                            jdbc.queryForObject(
                                "SELECT id FROM identity.users WHERE id=? FOR UPDATE",
                                UUID.class,
                                id);
                            locked.countDown();
                            await(release);
                          }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var request = executor.submit(() -> restore(ticket).getStatus());
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(1000);
        release.countDown();
        assertThat(request.get(5, TimeUnit.SECONDS)).isEqualTo(401);
        locker.get(5, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
    }
    assertThat(redis.hasKey(key(ticket))).isTrue();
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
  }

  @Test
  void anonymizationWinningUserLockCannotBeUndoneByWaitingRestore() throws Exception {
    String email = deleted("SELF", 1, false);
    jdbc.update(
        "UPDATE identity.users SET deactivated_at=clock_timestamp()-interval '30 days'+interval '2 seconds' WHERE email=?",
        email);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    UUID id = (UUID) user(email).get("id");
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch entered = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              entered.countDown();
              return invocation.callRealMethod();
            })
        .when(accounts)
        .lockById(id);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var job =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          status -> {
                            jdbc.queryForObject(
                                "SELECT id FROM identity.users WHERE id=? FOR UPDATE",
                                UUID.class,
                                id);
                            locked.countDown();
                            await(release);
                            markExpiredAnonymized(id);
                          }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var request = executor.submit(() -> restore(ticket).getStatus());
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(2100);
        release.countDown();
        assertThat(request.get(5, TimeUnit.SECONDS)).isEqualTo(401);
        job.get(5, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
    }
    assertThat(user(email).get("anonymized_at")).isNotNull();
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
  }

  @Test
  void ticketCannotAuthorizeProtectedApiAndIsNeverWrittenToLogs(CapturedOutput output)
      throws Exception {
    String email = deleted("SELF", 1, false);
    String ticket = ticket(login(email, PASSWORD, "/login"));
    assertThat(
            mvc.perform(
                    get("/api/v1/identity/me/profile").header("Authorization", "Bearer " + ticket))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    assertThat(output.getAll()).contains("Account is deactivated").doesNotContain(ticket);
    assertThat(user(email)).containsEntry("status", "DEACTIVATED");
  }

  private String deleted(String source, int days, boolean anonymized) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        """
        INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,
          deactivation_source,deactivated_at,anonymized_at)
        SELECT ?,?,?,'QA',id,'DEACTIVATED',?,clock_timestamp() - ? * interval '1 day',
          CASE WHEN ? THEN clock_timestamp() ELSE NULL END FROM identity.roles WHERE code='STUDENT'
        """,
        UUID.randomUUID(),
        email,
        HASH,
        source,
        days,
        anonymized);
    return email;
  }

  private void markExpiredAnonymized(UUID id) {
    jdbc.update(
        "UPDATE identity.users SET anonymized_at=clock_timestamp() WHERE id=?"
            + " AND status='DEACTIVATED' AND deactivation_source='SELF'"
            + " AND deactivated_at<=clock_timestamp()-interval '30 days'",
        id);
  }

  private Map<String, Object> user(String email) {
    return jdbc.queryForMap("SELECT * FROM identity.users WHERE email=?", email);
  }

  private int sessionCount(String email) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.refresh_tokens WHERE user_id=?",
        Integer.class,
        user(email).get("id"));
  }

  private Instant dbNow() {
    return jdbc.queryForObject("SELECT clock_timestamp()", Timestamp.class).toInstant();
  }

  private static String key(String ticket) {
    return "identity:restore:" + RestorationTicketSecrets.hash(ticket);
  }

  private static String ticket(MockHttpServletResponse response) throws Exception {
    assertError(response, 403, "IDT-104");
    assertThat(JsonPath.<Integer>read(response.getContentAsString(), "$.details.expiresInSeconds"))
        .isEqualTo(300);
    return JsonPath.read(response.getContentAsString(), "$.details.restoreTicket");
  }

  private MockHttpServletResponse login(String email, String password, String route)
      throws Exception {
    return postJson(route, "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
  }

  private MockHttpServletResponse restore(String ticket) throws Exception {
    return postJson("/deactivation/restore", "{\"restoreTicket\":\"" + ticket + "\"}");
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

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Test lock was not released");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Test lock interrupted", ex);
    }
  }
}
