package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringView;

/** Authoring detail #33 (DD F2 section 34); also the response body of #35 and #36. */
public interface GetProblemForAuthoringUseCase {
  ProblemAuthoringView get(ProblemAuthoringQuery query);
}
