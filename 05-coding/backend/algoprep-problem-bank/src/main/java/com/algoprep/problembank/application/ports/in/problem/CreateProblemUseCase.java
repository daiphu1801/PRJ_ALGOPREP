package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.CreateProblemCommand;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;

/** Creates a draft problem #35 (DD F2 section 36), filling missing limits from the defaults. */
public interface CreateProblemUseCase {
  ProblemContentSnapshot create(CreateProblemCommand command);
}
