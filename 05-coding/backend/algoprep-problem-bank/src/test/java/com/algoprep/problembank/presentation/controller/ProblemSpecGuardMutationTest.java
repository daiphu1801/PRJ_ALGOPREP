package com.algoprep.problembank.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.dto.SaveProblemSpecResult;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ProblemSpecGuardMutationTest {
  @TempDir Path temporary;

  @Configuration(proxyBeanMethods = false)
  @EnableMethodSecurity
  static class Security {}

  @Test
  void removingFunctionUpdateGuardBreaksPermissionOracle() throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/problembank/presentation/controller/ProblemSpecController.java");
    String source = Files.readString(file);
    String mutant =
        source.replace("@PreAuthorize(\"hasAuthority('PROBLEM_AUTHORING:UPDATE')\")", "");
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, false);
    candidate(mutant, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT SPEC_FUNCTION_UPDATE; original denied; mutant allowed; source unchanged");
  }

  private void candidate(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("ProblemSpecController.java");
    Files.writeString(
        file,
        source.replace(
            "package com.algoprep.problembank.presentation.controller;",
            "package com.algoprep.problembank.presentation.controller.mutationcopy;"));
    var errors = new ByteArrayOutputStream();
    int code =
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
                file.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
            new URLClassLoader(
                new java.net.URL[] {folder.toUri().toURL()}, getClass().getClassLoader());
        var context = new AnnotationConfigApplicationContext()) {
      Class<?> type =
          loader.loadClass(
              "com.algoprep.problembank.presentation.controller.mutationcopy.ProblemSpecController");
      context.setClassLoader(loader);
      context.register(Security.class);
      var useCase = mock(SaveProblemSpecUseCase.class);
      var bean = new RootBeanDefinition(type);
      bean.setInstanceSupplier(() -> controller(type, useCase));
      context.registerBeanDefinition("candidate", bean);
      context.refresh();
      invoke(type, context.getBean("candidate"), useCase, mutant);
    }
  }

  private Object controller(Class<?> type, SaveProblemSpecUseCase useCase) {
    try {
      var constructor = type.getDeclaredConstructor(SaveProblemSpecUseCase.class);
      constructor.setAccessible(true);
      return constructor.newInstance(useCase);
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException(failure);
    }
  }

  private void invoke(Class<?> type, Object bean, SaveProblemSpecUseCase useCase, boolean mutant)
      throws Exception {
    String id = UUID.randomUUID().toString();
    var jwt = Jwt.withTokenValue("fixture").header("alg", "none").subject(id).build();
    var caller = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    var result = new SaveProblemSpecResult(null, false, List.of(), Instant.EPOCH);
    when(useCase.save(any())).thenReturn(result);
    var method =
        type.getDeclaredMethod("save", JwtAuthenticationToken.class, String.class, String.class);
    method.setAccessible(true);
    SecurityContextHolder.getContext().setAuthentication(caller);
    try {
      assertThat(
              method.invoke(bean, caller, id, "{\"expectedUpdatedAt\":\"2026-11-02T00:00:00Z\"}"))
          .isEqualTo(result);
      assertThat(mutant).as("Original must deny an administrator lacking UPDATE").isTrue();
      verify(useCase).save(any());
    } catch (InvocationTargetException failure) {
      assertThat(mutant).isFalse();
      assertThat(failure.getCause()).isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
