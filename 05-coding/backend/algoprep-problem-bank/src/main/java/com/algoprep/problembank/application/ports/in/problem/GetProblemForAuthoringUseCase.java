package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.ProblemAuthoringView;
import com.algoprep.problembank.application.dto.problem.ProblemAuthoringQuery;

/** Authoring detail #33 (DD F2 section 34); also the response body of #35 and #36. */
public interface GetProblemForAuthoringUseCase {
  ProblemAuthoringView get(ProblemAuthoringQuery query);
}
