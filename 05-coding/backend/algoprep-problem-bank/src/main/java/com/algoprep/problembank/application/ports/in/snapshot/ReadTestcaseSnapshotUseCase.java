package com.algoprep.problembank.application.ports.in.snapshot;

import com.algoprep.problembank.domain.model.snapshot.TestcaseSnapshot;
import java.util.UUID;

/**
 * Exact pinned version only. Missing/corrupt data fails; current testcase fallback is forbidden.
 */
public interface ReadTestcaseSnapshotUseCase {
  TestcaseSnapshot loadTestcaseSnapshot(UUID problemId, int version);
}
