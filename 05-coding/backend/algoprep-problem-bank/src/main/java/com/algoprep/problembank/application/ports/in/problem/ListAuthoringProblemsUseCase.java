package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.ListProblemsQuery;
import com.algoprep.problembank.domain.model.problem.AuthoringListPage;

/** Management list #24 (DD F2 section 25). */
public interface ListAuthoringProblemsUseCase {
  AuthoringListPage list(ListProblemsQuery query);
}
