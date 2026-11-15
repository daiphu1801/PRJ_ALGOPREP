package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemStatsView;

/** Endpoint #40, DD F2 section 41; read only. */
public interface GetProblemStatsUseCase {
  ProblemStatsView get(ProblemAuthoringQuery query);
}
