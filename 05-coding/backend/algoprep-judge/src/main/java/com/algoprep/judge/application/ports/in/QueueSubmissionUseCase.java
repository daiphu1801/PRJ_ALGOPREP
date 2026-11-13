package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;

/**
 * Trusted STUDENT caller supplies its authenticated user id. Admission commits before publishing;
 * confirmed dispatch and queue-failure finalization are returned for the future HTTP/STOMP caller.
 * This kernel does not expose an incomplete public submission endpoint.
 */
public interface QueueSubmissionUseCase {
  QueuedSubmissionResult queue(RecordPendingSubmissionCommand command);
}
