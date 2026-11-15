package com.algoprep.judge.application.ports.in.submission;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.dto.submission.PendingSubmissionTicket;

/**
 * Trusted admission kernel, not an HTTP endpoint. Caller authenticates a STUDENT and supplies only
 * that user's id. The independent transaction commits before returning. A future submission
 * orchestrator must then publish with confirms and finalize queue failures; no runtime caller
 * exists until that complete path is implemented.
 */
public interface RecordPendingSubmissionUseCase {
  PendingSubmissionTicket recordPending(RecordPendingSubmissionCommand command);
}
