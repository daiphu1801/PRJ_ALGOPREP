package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

/** Real PostgreSQL, Redis and SMTP: BR-01 and the local mail transport of slice 6b. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
class LoginInfrastructureTest {
  private static final String PASSWORD = "ValidPass123";
  private static final String IP = "192.0.2.20";
  private static final String BASE = "/api/v1/identity/auth";

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
  @Autowired private LoginAttemptPort attempts;
  @Autowired private EmailPort mail;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void fifthFailureStill401ThenBothPortalsBlockAndIgnoreForwardedHeader() throws Exception {
    String email = account();
    for (int i = 0; i < 5; i++) {
      assertThat(login(email, "wrong", IP, "/login").getStatus()).isEqualTo(401);
    }
    String key = key(email);
    assertThat(redis.opsForValue().get(key)).isEqualTo("5");
    Long ttl = redis.getExpire(key);
    assertThat(ttl).isBetween(880L, 900L);
    for (String route : List.of("/login", "/instructor/login")) {
      MockHttpServletResponse response = login(email.toUpperCase(), PASSWORD, IP, route);
      assertThat(response.getStatus()).isEqualTo(429);
      assertThat(response.getContentAsString()).contains("IDT-103");
      assertThat(response.getHeader("Set-Cookie")).isNull();
    }
    assertThat(redis.opsForValue().get(key)).isEqualTo("5");
    assertThat(login(email, PASSWORD, "192.0.2.21", "/login").getStatus()).isEqualTo(200);
    assertThat(login(UUID.randomUUID() + "@example.test", "wrong", IP, "/login").getStatus())
        .isEqualTo(401);
  }

  @Test
  void successClearsCounterAndExpiredCounterAllowsNewLogin() throws Exception {
    String email = account();
    redis.opsForValue().set(key(email), "4", Duration.ofMinutes(15));
    assertThat(login(email, PASSWORD, IP, "/login").getStatus()).isEqualTo(200);
    assertThat(redis.hasKey(key(email))).isFalse();
    assertThat(login(email, "wrong", IP, "/login").getStatus()).isEqualTo(401);
    assertThat(redis.opsForValue().get(key(email))).isEqualTo("1");
    redis.opsForValue().set(key(email), "5", Duration.ofMillis(150));
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    while (Boolean.TRUE.equals(redis.hasKey(key(email))) && System.nanoTime() < deadline) {
      Thread.sleep(30);
    }
    assertThat(login(email, PASSWORD, IP, "/login").getStatus()).isEqualTo(200);
  }

  @Test
  void concurrentFailuresAreCountedWithoutExtendingFirstExpiry() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    redis.opsForValue().set(key(email), "4", Duration.ofSeconds(60));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<?>> jobs = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        jobs.add(executor.submit(() -> attempts.recordFailure(email, IP)));
      }
      for (Future<?> job : jobs) {
        job.get();
      }
    }
    assertThat(redis.opsForValue().get(key(email))).isEqualTo("24");
    assertThat(redis.getExpire(key(email))).isBetween(50L, 60L);
    assertThat(attempts.isBlocked(email, IP)).isTrue();
  }

  @Test
  void smtpAdapterDeliversToMailpit() throws Exception {
    String subject = "AlgoPrep SMTP smoke " + UUID.randomUUID();
    mail.send("qa@example.test", subject, "Local mail transport verification");
    try (HttpClient client = HttpClient.newHttpClient()) {
      var request =
          HttpRequest.newBuilder(
                  URI.create(
                      "http://"
                          + MAIL.getHost()
                          + ":"
                          + MAIL.getMappedPort(8025)
                          + "/api/v1/messages"))
              .GET()
              .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).contains(subject, "qa@example.test");
    }
  }

  @Test
  void redisTimeoutFailsOpenForCorrectAndIncorrectCredentials() throws Exception {
    String email = account();
    REDIS.execInContainer("redis-cli", "CLIENT", "PAUSE", "2500", "ALL");
    try {
      assertThat(login(email, PASSWORD, IP, "/login").getStatus()).isEqualTo(200);
      assertThat(login(email, "wrong", IP, "/login").getStatus()).isEqualTo(401);
    } finally {
      // redis-cli waits until CLIENT PAUSE ends, so the next test sees a healthy server.
      assertThat(REDIS.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }
  }

  private MockHttpServletResponse login(String email, String password, String ip, String route)
      throws Exception {
    return mvc.perform(
            post(BASE + route)
                .with(
                    request -> {
                      request.setRemoteAddr(ip);
                      return request;
                    })
                .header("X-Forwarded-For", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn()
        .getResponse();
  }

  private String account() {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status)"
            + " SELECT ?,?,?,?,id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        UUID.randomUUID(),
        email,
        new BCryptPasswordEncoder().encode(PASSWORD),
        "QA");
    return email;
  }

  private static String key(String email) {
    return "identity:ratelimit:login:" + email + ":" + IP;
  }
}
