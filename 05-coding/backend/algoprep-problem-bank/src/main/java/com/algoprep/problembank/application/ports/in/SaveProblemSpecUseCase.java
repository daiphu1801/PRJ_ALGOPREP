package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.dto.SaveProblemSpecResult;

/** PUT /authoring/problems/{problemId}/spec, DD F2 section 38. */
public interface SaveProblemSpecUseCase {
  SaveProblemSpecResult save(SaveProblemSpecCommand command);
}
