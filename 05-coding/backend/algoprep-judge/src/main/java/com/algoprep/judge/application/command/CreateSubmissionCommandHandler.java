package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.dto.SubmissionFinishedProgress;
import com.algoprep.judge.application.ports.in.CreateSubmissionUseCase;
import com.algoprep.judge.application.ports.in.QueueSubmissionUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Admission and queue-failure transactions finish before any public response or final push. */
@Service
class CreateSubmissionCommandHandler implements CreateSubmissionUseCase {
  private final QueueSubmissionUseCase queue;
  private final SubmissionProgressPort progress;

  CreateSubmissionCommandHandler(QueueSubmissionUseCase queue, SubmissionProgressPort progress) {
    this.queue = queue;
    this.progress = progress;
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public QueuedSubmissionResult create(RecordPendingSubmissionCommand command) {
    var result = queue.queue(command);
    if (result.queueUnavailable()) {
      var saved = result.queueFailure().submission();
      progress.afterCommit(
          saved.owner().userId(),
          new SubmissionFinishedProgress(
              saved.submissionId(),
              saved.timing().finishedAt(),
              saved.status(),
              null,
              null,
              null,
              null,
              null,
              null,
              saved.timing().finishedAt(),
              null));
      throw new SubmissionAdmissionException(
          "JDG-503", Map.of("submissionId", saved.submissionId()));
    }
    // A lost confirmation after worker pickup must never replace its state or emit a final push.
    return result;
  }
}
