package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class SubmissionAdmissionValidation {
  private SubmissionAdmissionValidation() {}

  static Program validate(RecordPendingSubmissionCommand command) {
    Objects.requireNonNull(command);
    Objects.requireNonNull(command.owner());
    Objects.requireNonNull(command.owner().userId());
    if (command.owner().problemId() == null) {
      throw new SubmissionAdmissionException("JDG-101");
    }
    Program program = Objects.requireNonNull(command.program());
    if (program.sourceCode() == null || program.sourceCode().isBlank()) {
      throw new SubmissionAdmissionException("JDG-V01");
    }
    if (program.sourceCode().getBytes(StandardCharsets.UTF_8).length > 65536) {
      throw new SubmissionAdmissionException("JDG-V02");
    }
    String language = choice(program.language(), Set.of("JAVA", "CPP", "PYTHON"), "JDG-V03");
    String model =
        choice(program.submissionModel(), Set.of("FUNCTION_WRAPPER", "STANDARD_IO"), "JDG-V04");
    String method = choice(program.sourceInputMethod(), Set.of("EDITOR", "FILE_UPLOAD"), "JDG-V05");
    if (method.equals("FILE_UPLOAD")) {
      validateFile(command.fileName(), language);
    }
    return new Program(language, model, program.sourceCode(), method);
  }

  private static String choice(String value, Set<String> allowed, String code) {
    String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
    if (!allowed.contains(normalized)) {
      throw new SubmissionAdmissionException(code);
    }
    return normalized;
  }

  private static void validateFile(String fileName, String language) {
    String extension =
        switch (language) {
          case "JAVA" -> ".java";
          case "CPP" -> ".cpp";
          default -> ".py";
        };
    if (fileName == null
        || fileName.length() > 255
        || !fileName.toLowerCase(Locale.ROOT).endsWith(extension)) {
      throw new SubmissionAdmissionException("JDG-V06");
    }
  }
}
