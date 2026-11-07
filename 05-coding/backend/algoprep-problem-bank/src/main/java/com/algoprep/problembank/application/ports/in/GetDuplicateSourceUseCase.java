package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.DuplicateSourceView;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;

/** Endpoint #34, DD F2 section 35; read only. */
public interface GetDuplicateSourceUseCase {
  DuplicateSourceView get(ProblemAuthoringQuery query);
}
