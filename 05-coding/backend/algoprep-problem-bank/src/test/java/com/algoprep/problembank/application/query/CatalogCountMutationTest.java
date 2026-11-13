package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CatalogCountMutationTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void removingAdminConditionIsCaughtForBothCountQueries(CatalogKind kind) throws Exception {
    String name =
        kind == CatalogKind.LEVEL
            ? "ListProblemLevelsQueryHandler"
            : "ListProblemTopicsQueryHandler";
    Path file = Path.of("src/main/java/com/algoprep/problembank/application/query", name + ".java");
    String source = Files.readString(file);
    runCopy(name, kind, source, false);
    String changed =
        source.replace("query.admin() && query.includeCounts()", "query.includeCounts()");
    assertThat(changed).isNotEqualTo(source);
    runCopy(name, kind, changed, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT " + kind + "_COUNTS; original GREEN; mutant oracle RED; source unchanged");
  }

  private void runCopy(String name, CatalogKind kind, String source, boolean mutant)
      throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutated" : "original"));
    Path java = folder.resolve(name + ".java");
    String copyPackage = "com.algoprep.problembank.application.query.mutationcopy";
    Files.writeString(
        java,
        source.replace(
            "package com.algoprep.problembank.application.query;", "package " + copyPackage + ";"));
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
      Class<?> type = loader.loadClass(copyPackage + "." + name);
      if (mutant) {
        assertThatThrownBy(() -> oracle(type, kind)).isInstanceOf(AssertionError.class);
      } else {
        oracle(type, kind);
      }
    }
  }

  private void oracle(Class<?> type, CatalogKind kind) throws Exception {
    var repository = mock(CatalogRepository.class);
    var constructor = type.getDeclaredConstructor(CatalogRepository.class);
    constructor.setAccessible(true);
    Object handler = constructor.newInstance(repository);
    var method = type.getDeclaredMethod("list", CatalogQuery.class);
    method.setAccessible(true);
    method.invoke(handler, new CatalogQuery(false, true));
    verify(repository).list(kind, false);
  }
}
