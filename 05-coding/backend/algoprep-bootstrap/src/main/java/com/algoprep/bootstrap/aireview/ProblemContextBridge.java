package com.algoprep.bootstrap.aireview;

import com.algoprep.aireview.application.ports.out.ProblemContextPort;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.ProblemContext;
import com.algoprep.problembank.application.ports.in.ReadProblemContextUseCase;
import com.algoprep.problembank.domain.model.ProblemContextView;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Type translation only: problem-bank answers through {@link ReadProblemContextUseCase}. A problem
 * that is missing, unpublished or withdrawn (PRB-010) and any other failure becomes AIR-501 with
 * reason UPSTREAM_PORT (DEC-2026-1109-f2-problem-context-visibility). The view has no field for a
 * reference solution or a testcase, so none can reach F5.
 */
@Component
final class ProblemContextBridge implements ProblemContextPort {
  private final ReadProblemContextUseCase problems;

  ProblemContextBridge(ReadProblemContextUseCase problems) {
    this.problems = problems;
  }

  @Override
  public ProblemContext getContext(UUID problemId, String language) {
    try {
      ProblemContextView view = problems.getContext(problemId, language);
      return new ProblemContext(
          view.title(),
          view.statementMarkdown(),
          view.constraintsMarkdown(),
          view.topics(),
          view.difficulty(),
          signature(view.functionSignature()),
          view.knownOptimalComplexity(),
          view.aiBrief(),
          view.noFullCode(),
          view.socraticOnly());
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.UPSTREAM_PORT, failure);
    }
  }

  private static String signature(ProblemContextView.FunctionSignature s) {
    return s == null ? null : s.functionName() + "(" + s.parameters() + ") -> " + s.returnType();
  }
}
