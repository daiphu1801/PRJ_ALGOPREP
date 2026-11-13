package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.dto.SubmissionFinishedProgress;
import com.algoprep.judge.application.dto.SubmissionStatusProgress;
import com.algoprep.judge.application.dto.TestcaseProgress;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocketHandshakeException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Transport-only proof: real JWT, live RBAC, PostgreSQL transactions and network frames. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(SubmissionStompFlowTest.BrokerBarrier.class)
class SubmissionStompFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @LocalServerPort private int port;
  @Autowired private AccessTokenPort tokens;
  @Autowired private JwtEncoder encoder;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private SubmissionProgressPort progress;
  @Autowired private PlatformTransactionManager transactions;

  @Autowired
  @Qualifier("taskScheduler")
  private org.springframework.scheduling.TaskScheduler jobScheduler;

  @Autowired
  @Qualifier("submissionSocketExpiryScheduler")
  private org.springframework.scheduling.TaskScheduler expiryScheduler;

  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  @Test
  void applicationJobsKeepTheirOwnVirtualThreadScheduler() throws Exception {
    assertThat(jobScheduler).isNotSameAs(expiryScheduler);
    var virtual = new java.util.concurrent.CompletableFuture<Boolean>();
    jobScheduler.schedule(
        () -> virtual.complete(Thread.currentThread().isVirtual()), Instant.now());
    assertThat(virtual.get(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void eachStudentOnlyReceivesTheirOwnMessagesAcrossTwoSessions() throws Exception {
    Account a = account("STUDENT");
    Account b = account("STUDENT");
    try (var wa = connected(a.token());
        var wb = connected(b.token());
        var wa2 = connected(a.token())) {
      wa.subscribed("/user/queue/submissions");
      wb.subscribed("/user/queue/submissions");
      wa2.subscribed("/user/queue/submissions");
      UUID x = UUID.randomUUID();
      UUID y = UUID.randomUUID();
      progress.afterCommit(a.id(), new TestcaseProgress(x, Instant.now(), 1, "WA", 1, 0, 12));
      progress.afterCommit(b.id(), new TestcaseProgress(y, Instant.now(), 1, "AC", 1, 1, 12));
      assertThat(body(wa.next()).get("submissionId").asString()).isEqualTo(x.toString());
      assertThat(body(wa2.next()).get("submissionId").asString()).isEqualTo(x.toString());
      JsonNode bMessage = body(wb.next());
      assertThat(bMessage.get("submissionId").asString()).isEqualTo(y.toString());
      assertThat(bMessage.propertyNames())
          .containsExactlyInAnyOrder(
              "submissionId",
              "occurredAt",
              "type",
              "seq",
              "isFinal",
              "order",
              "verdict",
              "completedCount",
              "passedCount",
              "totalCount");
      wa.noMessage();
      wb.noMessage();
    }
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"invalid-token", ""})
  void invalidOrMissingJwtGetsOnlyGenericErrorThenClose(String token) throws Exception {
    try (var wire = StompWireClient.open(port)) {
      wire.connect(token);
      wire.deniedAndClosed();
    }
  }

  @Test
  void aWellFormedJwtWithAnInvalidSignatureCannotConnect() throws Exception {
    String token = account("STUDENT").token();
    int signature = token.lastIndexOf('.') + 1;
    String forged =
        token.substring(0, signature)
            + (token.charAt(signature) == 'A' ? 'B' : 'A')
            + token.substring(signature + 1);
    try (var wire = StompWireClient.open(port)) {
      wire.connect(forged);
      wire.deniedAndClosed();
    }
  }

  @Test
  void publicHandshakeDoesNotOpenSiblingHttpOrSockJsPaths() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    for (String path : new String[] {"/ws/info", "/ws/anything", "/api/v1/judge/anything"}) {
      var response =
          client.send(
              java.net.http.HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                  .GET()
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode()).isEqualTo(401);
    }
  }

  @Test
  void expiredSignedTokenIsRejectedEvenInsideDecoderClockSkew() throws Exception {
    Account a = account("STUDENT");
    try (var wire = StompWireClient.open(port)) {
      wire.connect(signed(a.id(), Instant.now().minusSeconds(1)));
      wire.deniedAndClosed();
    }
  }

  @Test
  void deactivatedAccountCannotConnectWithStillValidToken() throws Exception {
    Account a = account("STUDENT");
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
            + " deactivated_at=now() WHERE id=?",
        a.id());
    try (var wire = StompWireClient.open(port)) {
      wire.connect(a.token());
      wire.deniedAndClosed();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/topic/admin/queue",
        "/queue/submissions",
        "/user/queue/other",
        "/user/queue/submissions/guessed-id",
        "/user/another-user/queue/submissions",
        "/topic/**"
      })
  void studentCannotSubscribeOutsideExactOwnDestination(String destination) throws Exception {
    try (var wire = connected(account("STUDENT").token())) {
      wire.subscribe(destination);
      wire.deniedAndClosed();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"ADMIN", "INSTRUCTOR"})
  void nonStudentsCannotSubscribeToStudentChannel(String category) throws Exception {
    try (var wire = connected(account(category).token())) {
      wire.subscribe("/user/queue/submissions");
      wire.deniedAndClosed();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/app/anything",
        "/topic/admin/queue",
        "/queue/submissions",
        "/user/queue/submissions"
      })
  void allClientSendFramesAreRejected(String destination) throws Exception {
    try (var wire = connected(account("STUDENT").token())) {
      wire.send(
          "SEND\ndestination:"
              + destination
              + "\ncontent-type:application/json\n\n{\"status\":\"ACCEPTED\"}");
      wire.deniedAndClosed();
    }
  }

  @Test
  void adminNeedsLiveMatrixCellAtSubscribeEvenAfterConnect() throws Exception {
    Account admin = account("ADMIN");
    try (var wire = connected(admin.token())) {
      setQueueRead(false);
      try {
        wire.subscribe("/topic/admin/queue");
        wire.deniedAndClosed();
      } finally {
        setQueueRead(true);
      }
    }
    try (var wire = connected(admin.token())) {
      wire.subscribed("/topic/admin/queue");
    }
  }

  @Test
  void adminSocketClosesAtExactTokenExpiry() throws Exception {
    Account admin = account("ADMIN");
    Instant expiry =
        Instant.now().plusSeconds(3).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    try (var wire = connected(signed(admin.id(), expiry))) {
      wire.subscribed("/topic/admin/queue");
      assertThat(wire.awaitClose()).isEqualTo(1008);
      assertThat(Instant.now()).isAfterOrEqualTo(expiry.minusMillis(50));
    }
  }

  @Test
  void rollbackPublishesNothingAndCommitPublishesInOrder() throws Exception {
    Account a = account("STUDENT");
    UUID submission = UUID.randomUUID();
    try (var wire = connected(a.token())) {
      wire.subscribed("/user/queue/submissions");
      TransactionTemplate tx = new TransactionTemplate(transactions);
      tx.executeWithoutResult(
          status -> {
            jdbc.update("UPDATE identity.users SET display_name='rolled back' WHERE id=?", a.id());
            progress.afterCommit(
                a.id(), new SubmissionStatusProgress(submission, Instant.now(), "COMPILING", null));
            status.setRollbackOnly();
          });
      wire.noMessage();
      assertThat(
              jdbc.queryForObject(
                  "SELECT display_name FROM identity.users WHERE id=?", String.class, a.id()))
          .isEqualTo("STOMP test");
      tx.executeWithoutResult(
          status -> {
            jdbc.update("UPDATE identity.users SET display_name='committed' WHERE id=?", a.id());
            progress.afterCommit(
                a.id(), new SubmissionStatusProgress(submission, Instant.now(), "COMPILING", null));
          });
      assertThat(body(wire.next()).get("seq").asInt()).isEqualTo(1);
      tx.executeWithoutResult(
          status ->
              progress.afterCommit(
                  a.id(), new SubmissionStatusProgress(submission, Instant.now(), "JUDGING", 1)));
      tx.executeWithoutResult(
          status ->
              progress.afterCommit(
                  a.id(), new TestcaseProgress(submission, Instant.now(), 1, "AC", 1, 1, 1)));
      tx.executeWithoutResult(status -> progress.afterCommit(a.id(), systemError(submission)));
      assertThat(body(wire.next()).get("seq").asInt()).isEqualTo(2);
      assertThat(body(wire.next()).get("seq").asInt()).isEqualTo(3);
      JsonNode last = body(wire.next());
      assertThat(last.get("seq").asInt()).isEqualTo(4);
      assertThat(last.get("isFinal").asBoolean()).isTrue();
      assertThat(last.get("scoreRatio").isNull()).isTrue();
    }
  }

  @Test
  void crossOriginHandshakeIsRejected() {
    assertThatThrownBy(
            () ->
                HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .header("Origin", "https://untrusted.example")
                    .subprotocols("v12.stomp")
                    .buildAsync(URI.create("ws://localhost:" + port + "/ws"), new StompWireClient())
                    .get(5, TimeUnit.SECONDS))
        .hasCauseInstanceOf(WebSocketHandshakeException.class);
  }

  private static SubmissionFinishedProgress systemError(UUID submission) {
    return new SubmissionFinishedProgress(
        submission,
        Instant.now(),
        "SYSTEM_ERROR",
        1,
        1,
        java.math.BigDecimal.ONE,
        null,
        null,
        null,
        Instant.now(),
        1);
  }

  private StompWireClient connected(String token) throws Exception {
    StompWireClient wire = StompWireClient.open(port);
    wire.connect(token);
    assertThat(wire.next()).startsWith("CONNECTED\n").contains("version:1.2");
    return wire;
  }

  private JsonNode body(String frame) {
    assertThat(frame).startsWith("MESSAGE\n");
    return json.readTree(frame.substring(frame.indexOf("\n\n") + 2));
  }

  private Account account(String category) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'STOMP test',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        category);
    return new Account(
        id, tokens.issue(id, BaseCategory.valueOf(category), category, UUID.randomUUID()).value());
  }

  private String signed(UUID userId, Instant expiry) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(userId.toString())
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(expiry)
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private void setQueueRead(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE r.id=p.role_id AND f.id=p.function_id AND a.id=p.action_id"
            + " AND r.code='ADMIN' AND f.code='JUDGE_QUEUE_MONITOR' AND a.code='READ'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
  }

  private record Account(UUID id, String token) {}

  @TestConfiguration
  static class BrokerBarrier {
    @Bean
    SmartInitializingSingleton subscribeBarrier(
        @Qualifier("clientInboundChannel") ExecutorSubscribableChannel inbound) {
      return () ->
          inbound.addInterceptor(
              new ExecutorChannelInterceptor() {
                @Override
                public void afterMessageHandled(
                    Message<?> message,
                    MessageChannel channel,
                    MessageHandler handler,
                    Exception error) {
                  StompHeaderAccessor headers =
                      MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                  if (handler instanceof SimpleBrokerMessageHandler
                      && error == null
                      && headers != null
                      && headers.getCommand() == StompCommand.SUBSCRIBE) {
                    StompWireClient.brokerHandled(headers.getSubscriptionId());
                  }
                }
              });
    }
  }
}
