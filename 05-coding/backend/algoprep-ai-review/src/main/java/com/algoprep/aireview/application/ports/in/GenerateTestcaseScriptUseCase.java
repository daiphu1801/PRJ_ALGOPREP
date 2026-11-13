package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.TestcaseScriptGeneration;

/**
 * P3 {@code TestcaseScriptGenerationPort} (DD F5 section 32.2): returns generator and validator
 * script text. Nothing is executed here. Throws AIR-501 or AIR-502; never charges an hourly quota.
 */
public interface GenerateTestcaseScriptUseCase {
  TestcaseScriptGeneration.Output generate(TestcaseScriptGeneration.Input input);
}
