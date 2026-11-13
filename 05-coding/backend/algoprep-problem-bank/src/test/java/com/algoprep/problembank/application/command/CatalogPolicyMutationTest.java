package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CatalogPolicyMutationTest {
  enum Rule {
    ADMIN,
    LAST_LEVEL,
    REFERENCES,
    ORDER,
    AUDIT
  }

  @TempDir Path temporary;
  private static final String COPY =
      "com.algoprep.problembank.application.command.mutationcopy.CatalogMutationService";

  @ParameterizedTest
  @EnumSource(Rule.class)
  void eachBusinessGuardIsKilledOnACompiledCopy(Rule rule) throws Exception {
    Path source =
        Path.of(
            "src/main/java/com/algoprep/problembank/application/command/CatalogMutationService.java");
    String original = Files.readString(source);
    checkCopy(original, rule, false);
    checkCopy(weaken(original, rule), rule, true);
    assertThat(Files.readString(source)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT CATALOG_"
            + rule
            + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private String weaken(String source, Rule rule) {
    String changed =
        switch (rule) {
          case ADMIN -> source.replace("if (!command.admin())", "if (false)");
          case REFERENCES -> source.replace("if (count > 0)", "if (false)");
          case LAST_LEVEL ->
              source.replace(
                  "kind == CatalogKind.LEVEL && repository.list(kind, false).size() == 1", "false");
          case ORDER -> source.replaceFirst("(?s)if \\(ordered == null.*?\\) \\{", "if (false) {");
          case AUDIT ->
              source
                  .replace("audit.record(", "discardAudit(")
                  .replace(
                      "  private void record(",
                      "  private void discardAudit(CatalogAuditEntry entry) {}\n\n  private void record(");
        };
    assertThat(changed).isNotEqualTo(source);
    return changed;
  }

  private void checkCopy(String source, Rule rule, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutated" : "original"));
    Path java = folder.resolve("CatalogMutationService.java");
    Files.writeString(
        java,
        source.replace(
            "package com.algoprep.problembank.application.command;",
            "package com.algoprep.problembank.application.command.mutationcopy;"));
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
                java.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> type = loader.loadClass(COPY);
      if (mutant) {
        assertThatThrownBy(() -> oracle(type, rule)).isInstanceOf(AssertionError.class);
      } else {
        oracle(type, rule);
      }
    }
  }

  private void oracle(Class<?> type, Rule rule) throws Exception {
    var repository = mock(CatalogRepository.class);
    var audit = mock(ProblemAuditPort.class);
    var cache = mock(ProblemListVersionPort.class);
    var item = new CatalogItem(UUID.randomUUID(), "OLD", "Old", 1, null);
    CatalogKind kind = rule == Rule.LAST_LEVEL ? CatalogKind.LEVEL : CatalogKind.TOPIC;
    when(repository.list(kind, false)).thenReturn(List.of(item));
    when(repository.create(kind, "Fresh")).thenReturn(item);
    when(repository.references(kind, item.id())).thenReturn(rule == Rule.REFERENCES ? 1L : 0L);
    var ctor =
        type.getDeclaredConstructor(
            CatalogRepository.class, ProblemAuditPort.class, ProblemListVersionPort.class);
    ctor.setAccessible(true);
    Object service = ctor.newInstance(repository, audit, cache);
    var command =
        new CatalogCommand(UUID.randomUUID(), rule != Rule.ADMIN, item.id(), "Fresh", List.of());
    String operation =
        rule == Rule.ORDER
            ? "reorder"
            : rule == Rule.LAST_LEVEL || rule == Rule.REFERENCES ? "delete" : "create";
    var method = type.getDeclaredMethod(operation, CatalogKind.class, CatalogCommand.class);
    method.setAccessible(true);
    if (rule == Rule.AUDIT) {
      method.invoke(service, kind, command);
      verify(audit).record(any());
    } else {
      try {
        method.invoke(service, kind, command);
        throw new AssertionError("Missing business guard: " + rule);
      } catch (InvocationTargetException failure) {
        String expected =
            switch (rule) {
              case ADMIN -> "PRB-004";
              case LAST_LEVEL -> "PRB-033";
              case REFERENCES -> "PRB-031";
              default -> "PRB-049";
            };
        assertThat(failure.getCause())
            .isInstanceOfSatisfying(
                AlgoPrepException.class,
                error -> assertThat(error.errorCode()).isEqualTo(expected));
      }
    }
  }
}
