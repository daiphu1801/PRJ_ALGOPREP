package com.algoprep.problembank.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Supplier;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Scratch copies only: production security guards are never weakened on disk. */
class StoragePolicyMutationTest {
  enum Rule {
    KEY_PARENT,
    GET_EXPIRY,
    WRITE_RETRY,
    ERROR_DETAIL
  }

  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(Rule.class)
  void weakeningEachPolicyBreaksItsOracle(Rule rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/problembank/infrastructure/storage/MinioTestcaseObjectStore.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case KEY_PARENT -> source.replace("segment.equals(\"..\")", "false");
          case GET_EXPIRY -> source.replace("ttl.getSeconds() > maximumSeconds", "false");
          case WRITE_RETRY -> source.replace("retryRead ? 2 : 1", "2");
          case ERROR_DETAIL ->
              source.replace(
                  "catch (Exception failure) {",
                  "catch (Exception failure) { if (true) { throw new IllegalStateException(failure); }");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT STORAGE_"
            + rule
            + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private void candidate(String source, Rule rule, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("MinioTestcaseObjectStore.java");
    Files.writeString(
        file,
        source.replace(
            "package com.algoprep.problembank.infrastructure.storage;",
            "package com.algoprep.problembank.infrastructure.storage.mutationcopy;"));
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
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      if (mutant) {
        assertThatThrownBy(() -> oracle(loader, rule)).isInstanceOf(AssertionError.class);
      } else {
        oracle(loader, rule);
      }
    }
  }

  private void oracle(ClassLoader loader, Rule rule) throws Exception {
    var client = mock(MinioClient.class);
    when(client.getObject(any())).thenThrow(new IOException("private"));
    when(client.statObject(any())).thenThrow(new IOException("private"));
    when(client.putObject(any())).thenThrow(new IOException("private"));
    when(client.getPresignedObjectUrl(any())).thenReturn("signed");
    var type =
        loader.loadClass(
            "com.algoprep.problembank.infrastructure.storage.mutationcopy.MinioTestcaseObjectStore");
    Supplier<MinioClient> supplier = () -> client;
    var store =
        (TestcaseObjectStorePort)
            type.getConstructor(Supplier.class, Supplier.class, String.class)
                .newInstance(supplier, supplier, "algoprep-testcases");
    String key = "problems/a/testcases/b/input.txt";
    switch (rule) {
      case KEY_PARENT ->
          assertThatThrownBy(() -> store.get("problems/../private"))
              .isInstanceOf(IllegalArgumentException.class);
      case GET_EXPIRY ->
          assertThatThrownBy(() -> store.presignGet(key, Duration.ofSeconds(301)))
              .isInstanceOf(IllegalArgumentException.class);
      case WRITE_RETRY -> {
        assertThatThrownBy(() -> store.put(key, new byte[0]))
            .isInstanceOf(TestcaseStorageUnavailableException.class);
        verify(client).putObject(any());
      }
      case ERROR_DETAIL ->
          assertThatThrownBy(() -> store.head(key))
              .isInstanceOfSatisfying(
                  TestcaseStorageUnavailableException.class,
                  failure -> assertThat(failure.getCause()).isNull());
    }
  }
}
