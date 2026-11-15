package com.algoprep.problembank.application.ports.in.stats;

import com.algoprep.problembank.application.dto.problem.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.stats.ProblemStatsView;

/** Endpoint #40, DD F2 section 41; read only. */
public interface GetProblemStatsUseCase {
  ProblemStatsView get(ProblemAuthoringQuery query);
}
