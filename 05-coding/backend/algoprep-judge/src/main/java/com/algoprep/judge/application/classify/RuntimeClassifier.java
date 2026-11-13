package com.algoprep.judge.application.classify;

import com.algoprep.common.functionspec.Language;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;

/** Engine status precedes wrapper markers; an exit number alone is never a harness signal. */
public final class RuntimeClassifier {
  public Status classify(
      String engineStatus, int exit, String stderr, Model model, Language language) {
    return switch (engineStatus) {
      case "Time Limit Exceeded" -> Status.TLE;
      case "Memory Limit Exceeded" -> Status.MLE;
      case "Output Limit Exceeded", "Signalled" -> Status.RE;
      case "Accepted" -> exit == 0 ? Status.OK : Status.RE;
      case "Nonzero Exit Status" -> nonzero(exit, stderr, model, language);
      default -> throw new JudgeInfrastructureException();
    };
  }

  private Status nonzero(int exit, String stderr, Model model, Language language) {
    if (model == Model.FUNCTION_WRAPPER) {
      if (exit == 90 && stderr.equals("@@APH-HARNESS-INPUT-ERROR@@\n")) {
        return Status.INPUT_ERROR;
      }
      if (exit == 91 && stderr.equals("@@APH-HARNESS-OUTPUT-LIMIT@@\n")) {
        return Status.RE;
      }
      if (exit == 92
          && language == Language.PYTHON
          && stderr.equals("@@APH-HARNESS-SIGNATURE-ERROR@@\n")) {
        return Status.SIGNATURE_ERROR;
      }
    }
    return Status.RE;
  }
}
