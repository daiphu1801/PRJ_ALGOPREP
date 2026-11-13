package com.algoprep.problembank.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.domain.model.CatalogItem;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

/** Each of the eight controller method guards is weakened independently on a source copy. */
class CatalogGuardMutationTest {
  private static final String PACKAGE =
      "com.algoprep.problembank.presentation.controller.mutationcopy";
  @TempDir Path temporary;

  @Configuration(proxyBeanMethods = false)
  @EnableMethodSecurity
  static class MethodSecurity {}

  static Stream<Arguments> guards() {
    return Stream.of("Levels", "Topics")
        .flatMap(
            kind ->
                Stream.of("create", "update", "reorder", "delete")
                    .map(method -> Arguments.of(kind, method)));
  }

  @ParameterizedTest
  @MethodSource("guards")
  void eachFunctionCellGuardIsCaughtWhenRemoved(String kind, String method) throws Exception {
    Path folder = Path.of("src/main/java/com/algoprep/problembank/presentation/controller");
    Path file = folder.resolve("Problem" + kind + "Controller.java");
    String original = Files.readString(file);
    runCopy(kind, method, original, false);
    String action =
        method.equals("create") ? "CREATE" : method.equals("delete") ? "DELETE" : "UPDATE";
    String pattern =
        "@PreAuthorize\\(\"hasAuthority\\('PROBLEM_AUTHORING:"
            + action
            + "'\\)\"\\)\\s*(?=(?:Item|Items|void) "
            + method
            + "\\()";
    var matcher = java.util.regex.Pattern.compile(pattern).matcher(original);
    assertThat(matcher.find()).isTrue();
    String changed = matcher.replaceFirst("");
    assertThat(changed).isNotEqualTo(original);
    runCopy(kind, method, changed, true);
    assertThat(Files.readString(file)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT "
            + kind
            + "."
            + method
            + "; source COPY only; original denied; mutant allowed");
  }

  private void runCopy(String kind, String method, String source, boolean mutant) throws Exception {
    String name = "Problem" + kind + "Controller";
    Path directory = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path java = directory.resolve(name + ".java");
    Path helper = directory.resolve("CatalogRequests.java");
    Files.writeString(java, isolate(source));
    Files.writeString(
        helper,
        isolate(
            Files.readString(
                Path.of(
                    "src/main/java/com/algoprep/problembank/presentation/controller/CatalogRequests.java"))));
    var errors = new ByteArrayOutputStream();
    int compiled =
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
                directory.toString(),
                java.toString(),
                helper.toString());
    assertThat(compiled).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {directory.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> type = loader.loadClass(PACKAGE + "." + name);
      try (var context = new AnnotationConfigApplicationContext()) {
        context.setClassLoader(loader);
        context.register(MethodSecurity.class);
        var bean = new RootBeanDefinition(type);
        bean.setInstanceSupplier(() -> controller(type));
        context.registerBeanDefinition("candidate", bean);
        context.refresh();
        assertInvocation(type, context.getBean("candidate"), method, kind, mutant);
      }
    }
  }

  private String isolate(String source) {
    return source.replace(
        "package com.algoprep.problembank.presentation.controller;", "package " + PACKAGE + ";");
  }

  private Object controller(Class<?> type) {
    try {
      var constructor = type.getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      var item = new CatalogItem(UUID.randomUUID(), "UNIT", "Unit", 1, null);
      Object[] ports = new Object[constructor.getParameterCount()];
      for (int i = 0; i < ports.length; i++) {
        ports[i] = mock(constructor.getParameterTypes()[i]);
        if (i > 0) {
          var execute =
              constructor.getParameterTypes()[i].getMethod("execute", CatalogCommand.class);
          when(execute.invoke(ports[i], org.mockito.ArgumentMatchers.any(CatalogCommand.class)))
              .thenReturn(List.of(item));
        }
      }
      return constructor.newInstance(ports);
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException(failure);
    }
  }

  private void assertInvocation(
      Class<?> type, Object bean, String name, String kind, boolean mutant) throws Exception {
    String id = UUID.randomUUID().toString();
    var jwt =
        Jwt.withTokenValue("unit")
            .header("alg", "none")
            .subject(id)
            .issuedAt(Instant.EPOCH)
            .expiresAt(Instant.ofEpochSecond(2000000000L))
            .build();
    var caller = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    var json = JsonMapper.builder().build();
    JsonNode body =
        json.readTree(
            name.equals("reorder")
                ? "{\"orderedIds\":[]}"
                : kind.equals("Levels") ? "{\"displayName\":\"Fresh\"}" : "{\"name\":\"Fresh\"}");
    Class<?>[] signature =
        name.equals("update")
            ? new Class<?>[] {JwtAuthenticationToken.class, String.class, JsonNode.class}
            : name.equals("delete")
                ? new Class<?>[] {JwtAuthenticationToken.class, String.class}
                : new Class<?>[] {JwtAuthenticationToken.class, JsonNode.class};
    Object[] args =
        name.equals("update")
            ? new Object[] {caller, id, body}
            : name.equals("delete") ? new Object[] {caller, id} : new Object[] {caller, body};
    var method = type.getDeclaredMethod(name, signature);
    method.setAccessible(true);
    SecurityContextHolder.getContext().setAuthentication(caller);
    try {
      method.invoke(bean, args);
      assertThat(mutant).as("Original must reject an ADMIN missing this cell").isTrue();
    } catch (InvocationTargetException failure) {
      assertThat(mutant)
          .as("Mutant must reach the contract oracle after successful compilation")
          .isFalse();
      assertThat(failure.getCause()).isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
