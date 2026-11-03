package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;

/**
 * BR-04 internal write core for DD F2 sections36/37; not a substitute for the complete HTTP
 * response.
 */
public interface SaveProblemContentUseCase {
  ProblemContentSnapshot save(SaveProblemContentCommand command);
}
