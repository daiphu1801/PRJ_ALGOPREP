package com.algoprep.problembank.application.ports.in.generation;

import com.algoprep.problembank.application.dto.generation.TestcaseGeneration;

/** Endpoints 55 and 56, DD F2 sections 56 and 57: the cap of AI generations per problem. */
public interface AiGenerationSettingsUseCase {
  TestcaseGeneration.Settings get();

  TestcaseGeneration.Settings update(TestcaseGeneration.UpdateSettings command);
}
