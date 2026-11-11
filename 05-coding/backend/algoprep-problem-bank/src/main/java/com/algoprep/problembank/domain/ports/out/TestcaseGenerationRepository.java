package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import java.util.UUID;

/** Persistence of BR-16 and of the cap setting; every call joins the caller transaction. */
public interface TestcaseGenerationRepository {
  /**
   * One conditional statement: used + 1 while used is below the cap. False when the cap was
   * reached.
   */
  boolean consumeQuota(UUID problemId);

  void insertRun(TestcaseGenerationRun run);

  /** Locks the settings singleton and returns its cap. */
  int lockCap();

  void updateCap(int maxPerProblem, UUID actorId);
}
