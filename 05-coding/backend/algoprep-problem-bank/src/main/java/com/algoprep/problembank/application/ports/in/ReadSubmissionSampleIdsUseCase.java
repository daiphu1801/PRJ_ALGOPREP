package com.algoprep.problembank.application.ports.in;

import java.util.Set;
import java.util.UUID;

/** Trusted current visibility lookup for F4's double Sample guard. No testcase content. */
public interface ReadSubmissionSampleIdsUseCase {
  Set<UUID> samples(UUID problemId, Set<UUID> testcaseIds);
}
