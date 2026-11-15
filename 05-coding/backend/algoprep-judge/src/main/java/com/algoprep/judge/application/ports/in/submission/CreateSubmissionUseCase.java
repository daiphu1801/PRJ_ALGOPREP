package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;

public interface CreateSubmissionUseCase {
  QueuedSubmissionResult create(RecordPendingSubmissionCommand command);
}
