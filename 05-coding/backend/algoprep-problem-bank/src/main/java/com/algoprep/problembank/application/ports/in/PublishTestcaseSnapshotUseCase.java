package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.SnapshotPublicationResult;
import java.util.UUID;

/**
 * Publishes the current approved grading set, reusing it when logically unchanged. The trusted
 * caller owns permission/checklist/sandbox/status/audit, locks the parent before any testcase
 * mutation, and calls this kernel in that same transaction. Parent-before-child lock order is
 * mandatory.
 */
public interface PublishTestcaseSnapshotUseCase {
  SnapshotPublicationResult publishSnapshot(UUID problemId);
}
