package com.algoprep.problembank.application.ports.in.grading;

import com.algoprep.problembank.application.dto.grading.ReferenceRunReport;

/** Runs the sample solution of a problem against its testcases, DD F2 section 48 (BR-15). */
public interface RunReferenceSolutionUseCase {
  ReferenceRunReport.Report run(ReferenceRunReport.Command command);
}
