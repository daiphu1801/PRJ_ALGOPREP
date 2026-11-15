package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ExportProblemsQuery;
import com.algoprep.problembank.application.dto.ExportedCsv;

/** Table export #27 (DD F2 section 28): table data only, never statements, specs or testcases. */
public interface ExportProblemsCsvUseCase {
  ExportedCsv export(ExportProblemsQuery query);
}
