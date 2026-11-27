package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.SaveProblemSpecCommand;
import com.algoprep.problembank.application.dto.problem.SaveProblemSpecResult;

/** PUT /authoring/problems/{problemId}/spec, DD F2 section 38. */
public interface SaveProblemSpecUseCase {
  SaveProblemSpecResult save(SaveProblemSpecCommand command);
}
