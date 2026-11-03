package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.application.dto.PendingSubmissionTicket;
import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.ports.in.CreateSubmissionUseCase;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Original oracle passes; each independently compiled mutation must fail the security oracle. */
class CreateSubmissionMutationTest {
  private static final String PACKAGE = "com.algoprep.judge.presentation.controller.mutationcopy";
  @TempDir Path temporary;

  @Configuration(proxyBeanMethods = false)
  @EnableMethodSecurity
  static class MethodSecurity {}

  @ParameterizedTest
  @ValueSource(strings = {"GUARD", "OWNER"})
  void securityMutationsAreCaughtWithoutChangingProductionSource(String rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/judge/presentation/controller/CreateSubmissionController.java");
    String original = Files.readString(file);
    assertThat(oracle(original, rule, "original")).isTrue();
    String changed =
        rule.equals("GUARD")
            ? original.replace("hasRole('STUDENT')", "isAuthenticated()")
            : original.replace(
                "UUID.fromString(caller.getToken().getSubject())",
                "UUID.fromString(text(body, \"userId\"))");
    assertThat(changed).isNotEqualTo(original);
    assertThat(oracle(changed, rule, "mutant")).isFalse();
    assertThat(Files.readString(file)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT CreateSubmission " + rule + "; original green; copy mutant red");
  }

  private boolean oracle(String source, String rule, String variant) throws Exception {
    Path dir = Files.createDirectories(temporary.resolve(rule + variant));
    Path java = dir.resolve("CreateSubmissionController.java");
    Files.writeString(
        java,
        source.replace(
            "package com.algoprep.judge.presentation.controller;", "package " + PACKAGE + ";"));
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
                dir.toString(),
                java.toString());
    assertThat(exit).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
            new URLClassLoader(
                new java.net.URL[] {dir.toUri().toURL()}, getClass().getClassLoader());
        var context = new AnnotationConfigApplicationContext()) {
      Class<?> type = loader.loadClass(PACKAGE + ".CreateSubmissionController");
      var captured = new AtomicReference<UUID>();
      context.setClassLoader(loader);
      context.register(MethodSecurity.class);
      var bean = new RootBeanDefinition(type);
      bean.setInstanceSupplier(() -> instance(type, captured));
      context.registerBeanDefinition("candidate", bean);
      context.refresh();
      return invoke(type, context.getBean("candidate"), rule, captured);
    }
  }

  private Object instance(Class<?> type, AtomicReference<UUID> captured) {
    try {
      var constructor = type.getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      CreateSubmissionUseCase port =
          command -> {
            captured.set(command.owner().userId());
            return new QueuedSubmissionResult(
                new PendingSubmissionTicket(
                    UUID.randomUUID(),
                    new PendingSubmissionTicket.ProgramType("PYTHON", "STANDARD_IO"),
                    Instant.EPOCH,
                    10),
                null);
          };
      return constructor.newInstance(port);
    } catch (ReflectiveOperationException error) {
      throw new IllegalStateException(error);
    }
  }

  private boolean invoke(Class<?> type, Object bean, String rule, AtomicReference<UUID> captured)
      throws Exception {
    UUID owner = UUID.randomUUID();
    var jwt = Jwt.withTokenValue("fixture").header("alg", "none").subject(owner.toString()).build();
    var caller =
        new JwtAuthenticationToken(
            jwt,
            List.of(
                new SimpleGrantedAuthority(
                    rule.equals("GUARD") ? "ROLE_INSTRUCTOR" : "ROLE_STUDENT")));
    JsonNode body =
        JsonMapper.builder()
            .build()
            .createObjectNode()
            .put("userId", UUID.randomUUID().toString())
            .put("sourceCode", "private")
            .put("language", "PYTHON")
            .put("submissionModel", "STANDARD_IO")
            .put("sourceInputMethod", "EDITOR");
    var method =
        type.getDeclaredMethod(
            "create", JwtAuthenticationToken.class, String.class, JsonNode.class);
    method.setAccessible(true);
    SecurityContextHolder.getContext().setAuthentication(caller);
    try {
      method.invoke(bean, caller, UUID.randomUUID().toString(), body);
      return !rule.equals("GUARD") && owner.equals(captured.get());
    } catch (InvocationTargetException error) {
      assertThat(error.getCause()).isInstanceOf(AccessDeniedException.class);
      return rule.equals("GUARD") && captured.get() == null;
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
