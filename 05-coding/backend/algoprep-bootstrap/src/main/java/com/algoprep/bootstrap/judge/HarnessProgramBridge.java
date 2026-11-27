package com.algoprep.bootstrap.judge;

import com.algoprep.harness.application.dto.MapCompileErrorCommand;
import com.algoprep.harness.application.dto.MatchOutputCommand;
import com.algoprep.harness.application.dto.MatchSpec;
import com.algoprep.harness.application.dto.MatchVerdict;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.MapCompileErrorUseCase;
import com.algoprep.harness.application.ports.in.MatchOutputUseCase;
import com.algoprep.harness.domain.model.CompileReportStatus;
import com.algoprep.harness.domain.model.LineMap;
import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.SourceMap;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Wiring and type translation only; F3 and F4 retain their own application contracts. */
@Component
public final class HarnessProgramBridge implements HarnessEvaluationPort {
  private final MapCompileErrorUseCase compiler;
  private final MatchOutputUseCase matcher;

  public HarnessProgramBridge(MapCompileErrorUseCase compiler, MatchOutputUseCase matcher) {
    this.compiler = compiler;
    this.matcher = matcher;
  }

  public Program program(PackagedProgram packaged) {
    if (!"go-judge".equals(packaged.payload().engine())) {
      throw new IllegalArgumentException("Unsupported judge engine");
    }
    var map = packaged.lineMap();
    return new Program(
        packaged.language(),
        Model.valueOf(packaged.model().name()),
        packaged.payload().body(),
        packaged.payload().artifact(),
        new SourceMap(
            map.prefixLineCount(),
            map.userLineCount(),
            map.callSiteStartLine(),
            map.callSiteEndLine(),
            map.sourceFileName()),
        packaged.signatureText(),
        packaged.resultSentinel());
  }

  @Override
  public CompileReport mapCompile(Program program, String compilerOutput) {
    var map = program.sourceMap();
    var report =
        compiler.map(
            new MapCompileErrorCommand(
                program.language(),
                SubmissionModel.valueOf(program.model().name()),
                compilerOutput,
                new LineMap(
                    map.prefixLineCount(),
                    map.userLineCount(),
                    map.callSiteStartLine(),
                    map.callSiteEndLine(),
                    map.sourceFileName()),
                program.signatureText(),
                UUID.randomUUID().toString()));
    if (report.status() == CompileReportStatus.HARNESS_INTERNAL) {
      return new CompileReport(true, null);
    }
    String text =
        report.diagnostics().stream()
            .limit(10)
            .map(diagnostic -> "Dòng " + diagnostic.line() + ": " + diagnostic.message())
            .collect(Collectors.joining("\n"));
    if (text.isEmpty()) {
      text = fixedMessage(report.fixedMessageKey(), program.signatureText());
    }
    return new CompileReport(false, text.substring(0, Math.min(10000, text.length())));
  }

  private String fixedMessage(String key, String signature) {
    if ("harness.compile.signatureMismatch".equals(key)) {
      return "Không tìm thấy hàm đúng chữ ký trong mã của bạn. Hãy kiểm tra tên hàm, số tham số và kiểu: "
          + signature
          + ".";
    }
    if ("harness.compile.noMain".equals(key)) {
      return "Chương trình chưa có hàm `main`.";
    }
    throw new IllegalStateException("Unexpected compiler report");
  }

  @Override
  public Match match(Program program, Matching matching, String stdout, String expected) {
    var result =
        matcher.match(
            new MatchOutputCommand(
                SubmissionModel.valueOf(program.model().name()),
                new MatchSpec(matching.strategy(), matching.epsilon(), matching.returnType()),
                program.resultSentinel(),
                stdout,
                expected));
    return new Match(
        result.verdict() == MatchVerdict.MATCH, result.actualOutput(), result.userConsole());
  }
}
