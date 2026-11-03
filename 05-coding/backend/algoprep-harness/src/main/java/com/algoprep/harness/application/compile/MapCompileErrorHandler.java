package com.algoprep.harness.application.compile;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.harness.application.dto.CompileErrorReport;
import com.algoprep.harness.application.dto.MapCompileErrorCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.MapCompileErrorUseCase;
import com.algoprep.harness.application.ports.out.CompilerDiagnosticLogPort;
import com.algoprep.harness.domain.compilation.CompileMapping;
import com.algoprep.harness.domain.model.LineMap;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** P3 orchestration: validates regions, bounds input and isolates the internal diagnostic log. */
@Service
final class MapCompileErrorHandler implements MapCompileErrorUseCase {
  private static final int MAX_OUTPUT_CHARS = 65536;
  private final CompilerDiagnosticLogPort log;

  MapCompileErrorHandler(CompilerDiagnosticLogPort log) {
    this.log = Objects.requireNonNull(log);
  }

  @Override
  public CompileErrorReport map(MapCompileErrorCommand command) {
    if (command == null
        || command.language() == null
        || command.model() == null
        || command.rawCompilerOutput() == null) {
      throw new DomainRuleViolationException("HRN-031", "Invalid compile mapping input");
    }
    validate(command.lineMap(), command.model());
    String raw = command.rawCompilerOutput();
    boolean truncated = raw.length() > MAX_OUTPUT_CHARS;
    String bounded = raw.substring(0, Math.min(raw.length(), MAX_OUTPUT_CHARS));
    writeLog(command.correlationId(), bounded, truncated);
    try {
      CompileMapping.Result result =
          CompileMapping.map(
              command.language(),
              command.model() == SubmissionModel.STANDARD_IO,
              newlines(bounded),
              command.lineMap());
      return new CompileErrorReport(
          result.status(), result.diagnostics(), result.fixedMessageKey(), command.correlationId());
    } catch (IllegalArgumentException | ArithmeticException internal) {
      throw new DomainRuleViolationException("HRN-031", "Compile diagnostic mapping failed");
    }
  }

  private static void validate(LineMap map, SubmissionModel model) {
    if (map == null
        || map.prefixLineCount() < 0
        || map.userLineCount() < 1
        || (long) map.prefixLineCount() + map.userLineCount() > Integer.MAX_VALUE
        || map.sourceFileName() == null
        || map.sourceFileName().isBlank()
        || map.sourceFileName().contains("/")
        || map.sourceFileName().contains("\\")) {
      throw invalidMap();
    }
    Integer start = map.callSiteStartLine();
    Integer end = map.callSiteEndLine();
    if ((start == null) != (end == null)) {
      throw invalidMap();
    }
    if (model == SubmissionModel.STANDARD_IO && (map.prefixLineCount() != 0 || start != null)) {
      throw invalidMap();
    }
    if (start != null
        && (start < 1
            || end < start
            || !(end <= map.prefixLineCount()
                || start > (long) map.prefixLineCount() + map.userLineCount()))) {
      throw invalidMap();
    }
  }

  private static DomainRuleViolationException invalidMap() {
    return new DomainRuleViolationException("HRN-040", "Inconsistent compile source regions");
  }

  private void writeLog(String correlationId, String bounded, boolean truncated) {
    try {
      log.write(correlationId, bounded, truncated);
    } catch (RuntimeException unavailable) {
      // The learner report is independent of the internal sink; never propagate raw log failures.
    }
  }

  private static String newlines(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }
}
