package com.algoprep.bootstrap.problembank;

import com.algoprep.aireview.application.dto.TestcaseScriptGeneration;
import com.algoprep.aireview.application.ports.in.GenerateTestcaseScriptUseCase;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.application.ports.out.TestcaseScriptGenerationPort;
import org.springframework.stereotype.Component;

/**
 * Type translation only: the AI subsystem answers through its P3 use case. The F2 request type has
 * no field for a sample solution, an author brief or a hidden testcase, and this bridge copies
 * nothing else, so none of them can reach the prompt. Every F5 failure (AIR-501, AIR-502 or
 * anything unexpected) passes through as an exception and becomes 503 PRB-051 on the F2 side.
 */
@Component
final class TestcaseScriptGenerationBridge implements TestcaseScriptGenerationPort {
  private final GenerateTestcaseScriptUseCase ai;

  TestcaseScriptGenerationBridge(GenerateTestcaseScriptUseCase ai) {
    this.ai = ai;
  }

  @Override
  public TestcaseGeneration.Scripts generate(TestcaseGeneration.ScriptRequest request) {
    var out =
        ai.generate(
            new TestcaseScriptGeneration.Input(
                request.callId(),
                request.userId(),
                request.problemId(),
                request.statementMarkdown(),
                request.constraintsMarkdown(),
                request.samples().stream()
                    .map(s -> new TestcaseScriptGeneration.Sample(s.input(), s.expectedOutput()))
                    .toList()));
    return new TestcaseGeneration.Scripts(
        out.generatorScript(),
        out.validatorScript(),
        out.categoryLabels(),
        out.modelId(),
        out.promptVersion());
  }
}
