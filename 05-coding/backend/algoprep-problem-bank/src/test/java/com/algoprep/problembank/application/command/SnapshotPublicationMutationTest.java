package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.in.ReadTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
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

class SnapshotPublicationMutationTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(strings = {"ID", "VISIBILITY", "POSITION", "INPUT", "OUTPUT", "VERIFY"})
  void copiedWriterCannotMissGradingChangesOrSkipObjectVerification(String rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/problembank/application/command/PublishTestcaseSnapshotCommandHandler.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "ID" -> source.replace("!a.testcaseId().equals(b.testcaseId())", "false");
          case "VISIBILITY" -> source.replace("!a.visibility().equals(b.visibility())", "false");
          case "POSITION" -> source.replace("a.position() != b.position()", "false");
          case "INPUT" -> source.replace("!Arrays.equals(a.input(), b.input())", "false");
          case "OUTPUT" ->
              source.replace("!Arrays.equals(a.expectedOutput(), b.expectedOutput())", "false");
          default -> source.replace("verifyObject(metadata, bytes.length);", "");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT SNAPSHOT_WRITE_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    var f = fixture(rule);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var type =
          loader.loadClass(
              "com.algoprep.problembank.application.command.mutationcopy.PublishTestcaseSnapshotCommandHandler");
      var constructor =
          type.getDeclaredConstructor(
              SnapshotPublicationRepository.class,
              TestcaseObjectStorePort.class,
              TestcaseSnapshotCodec.class,
              ReadTestcaseSnapshotUseCase.class,
              IdGeneratorPort.class);
      constructor.setAccessible(true);
      var writer =
          (PublishTestcaseSnapshotUseCase)
              constructor.newInstance(f.repository, f.storage, f.codec, f.snapshots, f.ids);
      Runnable oracle =
          rule.equals("VERIFY")
              ? () ->
                  PublishTestcaseSnapshotCommandHandlerTest.unavailable(
                      () -> writer.publishSnapshot(f.id))
              : () -> assertThat(writer.publishSnapshot(f.id).version()).isEqualTo(2);
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static SnapshotPublicationFixture fixture(String rule) {
    var f = new SnapshotPublicationFixture();
    if (rule.equals("VERIFY")) {
      doReturn(-1L).when(f.storage).head(anyString());
    } else {
      f.existing();
      when(f.repository.findApproved(f.id))
          .thenReturn(
              List.of(
                  SnapshotPublicationFixture.row(
                      rule.equals("ID") ? UUID.randomUUID() : f.testcaseId,
                      rule.equals("VISIBILITY") ? "SAMPLE" : "HIDDEN",
                      rule.equals("POSITION") ? 2 : 1,
                      rule.equals("INPUT") ? "different" : "input\r\n",
                      rule.equals("OUTPUT") ? "different" : "output\n")));
    }
    return f;
  }

  private Path compile(String source, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.application.command";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("PublishTestcaseSnapshotCommandHandler.java");
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
