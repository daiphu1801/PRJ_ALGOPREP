package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.DuplicateSourceView;
import com.algoprep.problembank.application.dto.problem.ProblemAuthoringQuery;

/** Endpoint #34, DD F2 section 35; read only. */
public interface GetDuplicateSourceUseCase {
  DuplicateSourceView get(ProblemAuthoringQuery query);
}
