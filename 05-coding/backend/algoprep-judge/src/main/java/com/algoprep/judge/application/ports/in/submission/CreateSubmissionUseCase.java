package com.algoprep.judge.application.ports.in.submission;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.dto.submission.QueuedSubmissionResult;

public interface CreateSubmissionUseCase {
  QueuedSubmissionResult create(RecordPendingSubmissionCommand command);
}
