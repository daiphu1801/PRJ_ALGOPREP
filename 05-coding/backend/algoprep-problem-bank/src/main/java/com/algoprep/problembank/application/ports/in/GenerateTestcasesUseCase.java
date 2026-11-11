package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.TestcaseGeneration;

/** Endpoint 48, DD F2 section 49 (BR-16): AI writes scripts, the sample solution writes outputs. */
public interface GenerateTestcasesUseCase {
  TestcaseGeneration.Result generate(TestcaseGeneration.Command command);
}
