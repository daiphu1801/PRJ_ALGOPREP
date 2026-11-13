package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import java.util.Optional;
import java.util.UUID;

/** Read side of the authoring screens (DD F2 section 34). */
public interface ProblemAuthoringReadRepository {

  /**
   * The problem with its relations, or empty when it does not exist or is soft-deleted. Testcase
   * bodies and object keys stay null unless {@code withTestcaseContent} is true.
   */
  Optional<AuthoringProblemRecord> find(UUID problemId, boolean withTestcaseContent);

  /** {@code max_ai_generations_per_problem} of the settings singleton. */
  int aiGenerationLimit();
}
