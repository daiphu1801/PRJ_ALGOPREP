package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.SubmissionReads.Progress;
import java.util.UUID;

/** Authorized read contract from F4 API sections 7-10 and 13-15. */
public interface GetSubmissionProgressUseCase {
  Progress getProgress(UUID owner, UUID submissionId);
}
