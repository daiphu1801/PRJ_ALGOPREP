package com.algoprep.problembank.application.ports.in.bulk;

import com.algoprep.problembank.application.dto.bulk.ExportProblemsQuery;
import com.algoprep.problembank.application.dto.bulk.ExportedCsv;

/** Table export #27 (DD F2 section 28): table data only, never statements, specs or testcases. */
public interface ExportProblemsCsvUseCase {
  ExportedCsv export(ExportProblemsQuery query);
}
