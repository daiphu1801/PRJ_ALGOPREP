package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.application.dto.TestcaseGeneration;

/**
 * Asks the AI subsystem for generator and validator scripts (DD F2 section 58.2, F5 P3). Any
 * failure, including an unusable answer, is an unchecked exception: the caller turns it into 503
 * PRB-051 and changes nothing. The request type has no field for the sample solution.
 */
public interface TestcaseScriptGenerationPort {
  TestcaseGeneration.Scripts generate(TestcaseGeneration.ScriptRequest request);
}
