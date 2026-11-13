package com.algoprep.problembank.application.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.exception.GradingDataException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.TestcaseData;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotIntegrityMutationTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(strings = {"CHECKSUM", "PINNED_VERSION"})
  void copiedCodecCannotBypassIntegrityOrPinnedVersion(String rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/problembank/application/snapshot/TestcaseSnapshotCodec.java");
    String source = Files.readString(file);
    String mutant =
        rule.equals("CHECKSUM")
            ? source.replace("!MessageDigest.isEqual(", "false && !MessageDigest.isEqual(")
            : source.replace(
                "integer(root.get(\"testcaseSetVersion\")) != metadata.version()", "false");
    assertThat(mutant).isNotEqualTo(source);
    checkCandidate(source, rule, false);
    checkCandidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT SNAPSHOT_" + rule + "; original GREEN; mutant RED");
  }

  private void checkCandidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    UUID id = UUID.randomUUID();
    var codec = new TestcaseSnapshotCodec();
    byte[] bytes =
        codec.encode(
            id,
            7,
            List.of(new TestcaseData(UUID.randomUUID(), "HIDDEN", 1, new byte[0], new byte[0])));
    var metadata =
        new SnapshotMetadata(
            id,
            rule.equals("CHECKSUM") ? 7 : 6,
            "unused",
            1,
            rule.equals("CHECKSUM") ? "0".repeat(64) : codec.sha256(bytes));
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var type =
          loader.loadClass(
              "com.algoprep.problembank.application.snapshot.mutationcopy.TestcaseSnapshotCodec");
      Object instance = type.getConstructor().newInstance();
      var method = type.getMethod("decode", SnapshotMetadata.class, byte[].class);
      Runnable oracle =
          () ->
              assertThatThrownBy(() -> method.invoke(instance, metadata, bytes))
                  .hasCauseInstanceOf(GradingDataException.class);
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private Path compile(String source, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.application.snapshot";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("TestcaseSnapshotCodec.java");
    Files.writeString(
        file, source.replace("package " + prefix + ";", "package " + prefix + ".mutationcopy;"));
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
    return folder;
  }
}
