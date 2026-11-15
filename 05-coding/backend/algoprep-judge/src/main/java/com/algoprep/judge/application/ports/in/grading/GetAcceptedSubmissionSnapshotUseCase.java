package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;
import java.util.UUID;

/** P4 has no ownership guard. The trusted AI caller must check the returned userId. */
public interface GetAcceptedSubmissionSnapshotUseCase {
  AcceptedSubmissionSnapshot getAccepted(UUID submissionId);
}
