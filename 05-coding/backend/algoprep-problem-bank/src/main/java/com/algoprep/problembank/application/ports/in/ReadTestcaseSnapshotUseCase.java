package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.TestcaseSnapshot;
import java.util.UUID;

/**
 * Exact pinned version only. Missing/corrupt data fails; current testcase fallback is forbidden.
 */
public interface ReadTestcaseSnapshotUseCase {
  TestcaseSnapshot loadTestcaseSnapshot(UUID problemId, int version);
}
