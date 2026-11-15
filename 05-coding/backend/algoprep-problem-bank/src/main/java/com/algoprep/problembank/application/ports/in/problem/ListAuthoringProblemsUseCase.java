package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ListProblemsQuery;
import com.algoprep.problembank.domain.model.AuthoringListPage;

/** Management list #24 (DD F2 section 25). */
public interface ListAuthoringProblemsUseCase {
  AuthoringListPage list(ListProblemsQuery query);
}
