package com.algoprep.judge.application.command;

import static com.algoprep.judge.application.command.RecordPendingSubmissionCommandHandlerTest.rejected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubmissionAdmissionValidationTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\r\n", "\u2003"})
  void blankSourceIsRejectedBeforeAnyRead(String source) {
    var f = new SubmissionAdmissionFixture();
    rejected(
        () -> f.handler().recordPending(f.command(source, "PYTHON", "STANDARD_IO", "EDITOR", null)),
        "JDG-V01");
    verifyNoInteractions(f.repository, f.problems, f.ids);
  }

  @ParameterizedTest
  @CsvSource({"a,65536,true", "a,65537,false", "đ,32768,true", "đ,32769,false"})
  void sourceLimitIsMeasuredInUtf8Bytes(String unit, int count, boolean allowed) {
    var f = new SubmissionAdmissionFixture();
    var command = f.command(unit.repeat(count), "PYTHON", "STANDARD_IO", "EDITOR", null);
    if (allowed) {
      assertThat(f.handler().recordPending(command).status()).isEqualTo("PENDING");
    } else {
      rejected(() -> f.handler().recordPending(command), "JDG-V02");
      verify(f.repository, never()).insert(any());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "RUST,STANDARD_IO,EDITOR,JDG-V03",
    "PYTHON,OTHER,EDITOR,JDG-V04",
    "PYTHON,STANDARD_IO,OTHER,JDG-V05"
  })
  void unsupportedChoicesNeverReachStorage(
      String language, String model, String method, String code) {
    var f = new SubmissionAdmissionFixture();
    rejected(
        () -> f.handler().recordPending(f.command("pass", language, model, method, null)), code);
    verifyNoInteractions(f.repository, f.problems);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  void missingChoicesHaveSpecificCodes(int field) {
    var f = new SubmissionAdmissionFixture();
    rejected(
        () ->
            f.handler()
                .recordPending(
                    f.command(
                        "pass",
                        field == 0 ? null : "PYTHON",
                        field == 1 ? null : "STANDARD_IO",
                        field == 2 ? null : "EDITOR",
                        null)),
        "JDG-V0" + (field + 3));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"Main.java", "main.py.exe", "main.py ", "file"})
  void uploadExtensionMustMatchTheLanguage(String name) {
    var f = new SubmissionAdmissionFixture();
    rejected(
        () ->
            f.handler()
                .recordPending(f.command("pass", "PYTHON", "STANDARD_IO", "FILE_UPLOAD", name)),
        "JDG-V06");
    verifyNoInteractions(f.repository, f.problems);
  }

  @ParameterizedTest
  @CsvSource({"java,Main.JAVA", "cpp,main.CpP", "python,main.PY"})
  void uploadSuffixAndLanguageAcceptCaseWithoutChangingSource(String language, String file) {
    var f = new SubmissionAdmissionFixture();
    var value =
        SubmissionAdmissionValidation.validate(
            f.command(" pass\r\n", language, "standard_io", "file_upload", file));
    assertThat(value.language()).isEqualTo(language.toUpperCase(java.util.Locale.ROOT));
    assertThat(value.sourceCode()).isEqualTo(" pass\r\n");
    assertThat(value.sourceInputMethod()).isEqualTo("FILE_UPLOAD");
  }

  @Test
  void filenameLimitIs255AndEditorIgnoresFilename() {
    var f = new SubmissionAdmissionFixture();
    assertThat(
            SubmissionAdmissionValidation.validate(
                f.command("pass", "PYTHON", "STANDARD_IO", "FILE_UPLOAD", "a".repeat(252) + ".py")))
        .isNotNull();
    rejected(
        () ->
            SubmissionAdmissionValidation.validate(
                f.command("pass", "PYTHON", "STANDARD_IO", "FILE_UPLOAD", "a".repeat(253) + ".py")),
        "JDG-V06");
    assertThat(
            SubmissionAdmissionValidation.validate(
                f.command("pass", "PYTHON", "STANDARD_IO", "EDITOR", "ignored.java")))
        .isNotNull();
    assertThat(f.command("pass", "PYTHON", "STANDARD_IO", "FILE_UPLOAD", "private.py").toString())
        .doesNotContain("private.py", "pass");
  }
}
