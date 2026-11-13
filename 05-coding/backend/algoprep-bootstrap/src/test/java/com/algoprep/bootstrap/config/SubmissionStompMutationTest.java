package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.WebSocketSession;

/** Mutates compiled source copies only; no delivery security configuration is weakened. */
class SubmissionStompMutationTest {
  private static final Path SOURCE = Path.of("src/main/java/com/algoprep/bootstrap/config");
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "STUDENT",
        "DESTINATION",
        "ADMIN_ROLE",
        "ADMIN_CELL",
        "SEND",
        "JWT",
        "EXPIRY",
        "EXPIRED"
      })
  void everyGuardMustBeObservedByAnOracle(String rule) throws Exception {
    String source = Files.readString(SOURCE.resolve("SubmissionStompInterceptor.java"));
    String mutant =
        switch (rule) {
          case "STUDENT" -> source.replace("has(auth, \"ROLE_STUDENT\")", "true");
          case "DESTINATION" ->
              source.replace(
                  "\"/user/queue/submissions\".equals(destination)",
                  "destination.startsWith(\"/user/\")");
          case "ADMIN_ROLE" -> source.replace("has(current, \"ROLE_ADMIN\")", "true");
          case "ADMIN_CELL" -> source.replace("has(current, \"JUDGE_QUEUE_MONITOR:READ\")", "true");
          case "SEND" ->
              source.replace(
                  "command != StompCommand.UNSUBSCRIBE",
                  "command != StompCommand.SEND && command != StompCommand.UNSUBSCRIBE");
          case "JWT" ->
              source.replace(
                  "decoder.decode(values.getFirst().substring(7))",
                  "Jwt.withTokenValue(\"forged\").header(\"alg\", \"RS256\").subject(\"00000000-0000-0000-0000-000000000001\")"
                      + ".expiresAt(Instant.now().plusSeconds(60)).build()");
          case "EXPIRED" -> source.replace("!expiresAt.isAfter(Instant.now())", "false");
          default -> source.replace("sessions.expireAdmin(sessionId, expiry);", "");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(SOURCE.resolve("SubmissionStompInterceptor.java")))
        .isEqualTo(source);
    System.out.println("MUTATION CAUGHT STOMP_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
      Object sessions = sessions(loader, scheduler);
      JwtDecoder decoder = mock(JwtDecoder.class);
      Converter<Jwt, AbstractAuthenticationToken> converter = token -> authentication(token, rule);
      var constructor =
          loader
              .loadClass("com.algoprep.bootstrap.config.mutationcopy.SubmissionStompInterceptor")
              .getDeclaredConstructor(JwtDecoder.class, Converter.class, sessions.getClass());
      constructor.setAccessible(true);
      ChannelInterceptor interceptor =
          (ChannelInterceptor) constructor.newInstance(decoder, converter, sessions);
      StompHeaderAccessor headers = headers(rule, decoder);
      headers.setLeaveMutable(true);
      var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
      Runnable oracle =
          () -> {
            if (rule.equals("EXPIRY")) {
              interceptor.preSend(message, null);
              verify(scheduler).schedule(any(Runnable.class), any(Instant.class));
            } else {
              assertThatThrownBy(() -> interceptor.preSend(message, null))
                  .isInstanceOf(MessageDeliveryException.class)
                  .hasMessage("Request denied")
                  .hasNoCause();
            }
          };
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static Object sessions(ClassLoader loader, ThreadPoolTaskScheduler scheduler)
      throws Exception {
    Class<?> type =
        loader.loadClass("com.algoprep.bootstrap.config.mutationcopy.SubmissionSocketSessions");
    var constructor = type.getDeclaredConstructor(ThreadPoolTaskScheduler.class);
    constructor.setAccessible(true);
    Object sessions = constructor.newInstance(scheduler);
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn("session");
    var opened = type.getDeclaredMethod("opened", WebSocketSession.class);
    opened.setAccessible(true);
    opened.invoke(sessions, socket);
    return sessions;
  }

  private static StompHeaderAccessor headers(String rule, JwtDecoder decoder) {
    Jwt token =
        Jwt.withTokenValue("signed")
            .header("alg", "RS256")
            .subject(UUID.randomUUID().toString())
            .expiresAt(
                rule.equals("EXPIRED")
                    ? Instant.now().minusSeconds(1)
                    : Instant.now().plusSeconds(60))
            .build();
    var headers =
        StompHeaderAccessor.create(
            rule.equals("SEND")
                ? StompCommand.SEND
                : rule.equals("JWT") || rule.equals("EXPIRED")
                    ? StompCommand.CONNECT
                    : StompCommand.SUBSCRIBE);
    headers.setSessionId("session");
    headers.setUser(authentication(token, rule));
    headers.setDestination(
        switch (rule) {
          case "STUDENT" -> "/user/queue/submissions";
          case "DESTINATION" -> "/user/queue/other";
          case "SEND" -> "/app/anything";
          default -> "/topic/admin/queue";
        });
    if (rule.equals("JWT") || rule.equals("EXPIRED")) {
      headers.setNativeHeader("Authorization", "Bearer signed");
      headers.setAcceptVersion("1.2");
      if (rule.equals("JWT")) {
        when(decoder.decode("signed")).thenThrow(new IllegalArgumentException("SECRET"));
      } else {
        when(decoder.decode("signed")).thenReturn(token);
      }
    }
    return headers;
  }

  private static JwtAuthenticationToken authentication(Jwt token, String rule) {
    String[] cells =
        switch (rule) {
          case "STUDENT" -> new String[] {"ROLE_INSTRUCTOR"};
          case "ADMIN_ROLE" -> new String[] {"ROLE_INSTRUCTOR", "JUDGE_QUEUE_MONITOR:READ"};
          case "ADMIN_CELL" -> new String[] {"ROLE_ADMIN"};
          case "EXPIRY" -> new String[] {"ROLE_ADMIN", "JUDGE_QUEUE_MONITOR:READ"};
          default -> new String[] {"ROLE_STUDENT"};
        };
    return new JwtAuthenticationToken(
        token, Arrays.stream(cells).map(SimpleGrantedAuthority::new).toList(), token.getSubject());
  }

  private Path compile(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    String prefix = "package com.algoprep.bootstrap.config;";
    for (String name : new String[] {"SubmissionStompInterceptor", "SubmissionSocketSessions"}) {
      String code =
          name.equals("SubmissionStompInterceptor")
              ? source
              : Files.readString(SOURCE.resolve(name + ".java"));
      Files.writeString(
          folder.resolve(name + ".java"),
          code.replace(prefix, prefix.replace(";", ".mutationcopy;")));
    }
    var errors = new ByteArrayOutputStream();
    int exit =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                errors,
                errors,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                folder.resolve("SubmissionStompInterceptor.java").toString(),
                folder.resolve("SubmissionSocketSessions.java").toString());
    assertThat(exit).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    return folder;
  }
}
