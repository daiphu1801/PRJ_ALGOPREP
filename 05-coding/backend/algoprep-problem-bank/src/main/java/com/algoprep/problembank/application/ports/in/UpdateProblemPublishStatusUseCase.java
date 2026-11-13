package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusResult;

/** Publish or withdraw a problem #39 (DD F2 section 40), with the BR-07 checklist on publish. */
public interface UpdateProblemPublishStatusUseCase {
  UpdateProblemStatusResult update(UpdateProblemStatusCommand command);
}
