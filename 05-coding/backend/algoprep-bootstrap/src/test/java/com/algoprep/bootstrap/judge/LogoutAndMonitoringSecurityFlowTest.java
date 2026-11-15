package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "judge.admin-stream.enabled=false",
      "management.endpoint.health.show-details=never",
      "management.endpoint.health.show-components=never",
      "algoprep.monitoring.bearer-token=test-scrape-token"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(QueueStreamBrokerBarrier.class)
class LogoutAndMonitoringSecurityFlowTest extends QueueStreamTestSupport {
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
  static void redis(org.springframework.test.context.DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void logoutImmediatelyRevokesOnlyItsFamilyAndClosesItsIdleSocket() throws Exception {
    jdbc.update(
        "UPDATE identity.users SET password_hash=? WHERE id=?",
        new BCryptPasswordEncoder().encode("RegressionPass123"),
        operator);
    var first = login();
    var second = login();
    String token1 = json.readTree(first.body()).path("accessToken").asText();
    String token2 = json.readTree(second.body()).path("accessToken").asText();
    String cookie1 = first.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    String cookie2 = second.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    try (var a = QueueStreamWireClient.connect(port, token1);
        var b = QueueStreamWireClient.connect(port, token2)) {
      a.subscribe("logout-first");
      b.subscribe("logout-second");
      ticks.broadcast();
      body(a.next());
      body(b.next());
      assertThat(send("POST", "/api/v1/identity/auth/logout", cookie1, null, "").statusCode())
          .isEqualTo(204);
      a.awaitClosed(); // No new batch is needed to trigger closure.
      assertThat(send("GET", "/api/v1/identity/me/profile", null, token1, null).statusCode())
          .isEqualTo(401);
      assertThat(send("GET", "/api/v1/identity/me/profile", null, token2, null).statusCode())
          .isEqualTo(200);
      ticks.broadcast();
      body(b.next());
      a.noMessage();
      assertThat(send("POST", "/api/v1/identity/auth/refresh", cookie1, null, "").statusCode())
          .isEqualTo(401);
      var rotated = send("POST", "/api/v1/identity/auth/refresh", cookie2, null, "");
      assertThat(rotated.statusCode()).isEqualTo(200);
      String token3 = json.readTree(rotated.body()).path("accessToken").asText();
      assertThat(send("GET", "/api/v1/identity/me/profile", null, token2, null).statusCode())
          .isEqualTo(200);
      // An older rotated cookie still identifies the same family for explicit logout.
      assertThat(send("POST", "/api/v1/identity/auth/logout", cookie2, null, "").statusCode())
          .isEqualTo(204);
      b.awaitClosed();
      assertThat(send("GET", "/api/v1/identity/me/profile", null, token3, null).statusCode())
          .isEqualTo(401);
    }
  }

  @Test
  void publicHealthHidesComponentsAndMetricsRequiresTheDedicatedCredential() throws Exception {
    var health = send("GET", "/actuator/health", null, null, null);
    assertThat(health.statusCode()).isEqualTo(200);
    assertThat(health.body()).doesNotContain("components", "diskSpace", "redis", "jdbc", "details");
    assertThat(send("GET", "/actuator/prometheus", null, null, null).statusCode()).isEqualTo(401);
    assertThat(send("GET", "/actuator/prometheus", null, "wrong-scrape-token", null).statusCode())
        .isEqualTo(401);
    var metrics = send("GET", "/actuator/prometheus", null, "test-scrape-token", null);
    assertThat(metrics.statusCode()).isEqualTo(200);
    assertThat(metrics.body()).contains("jvm_").doesNotContain("test-scrape-token");
  }

  private HttpResponse<String> login() throws Exception {
    var response =
        send(
            "POST",
            "/api/v1/identity/auth/login",
            null,
            null,
            "{\"email\":\"" + operator + "@example.test\",\"password\":\"RegressionPass123\"}");
    assertThat(response.statusCode()).isEqualTo(200);
    return response;
  }

  private HttpResponse<String> send(
      String method, String path, String cookie, String token, String body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (cookie != null) {
      request.header("Cookie", cookie);
    }
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    request.header("X-Requested-With", "XMLHttpRequest");
    if (body != null) {
      request.header("Content-Type", "application/json");
    }
    request.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
