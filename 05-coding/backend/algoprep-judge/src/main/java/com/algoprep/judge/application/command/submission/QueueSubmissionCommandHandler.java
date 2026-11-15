package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.in.QueueSubmissionUseCase;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.in.ResolveQueueFailureUseCase;
import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class QueueSubmissionCommandHandler implements QueueSubmissionUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(QueueSubmissionCommandHandler.class);
  private final RecordPendingSubmissionUseCase admission;
  private final SubmissionQueuePort queue;
  private final ResolveQueueFailureUseCase failure;

  QueueSubmissionCommandHandler(
      RecordPendingSubmissionUseCase admission,
      SubmissionQueuePort queue,
      ResolveQueueFailureUseCase failure) {
    this.admission = admission;
    this.queue = queue;
    this.failure = failure;
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public QueuedSubmissionResult queue(RecordPendingSubmissionCommand command) {
    var ticket = admission.recordPending(command);
    boolean confirmed;
    try {
      confirmed = queue.enqueue(ticket.submissionId());
    } catch (RuntimeException error) {
      confirmed = false;
    }
    var result =
        new QueuedSubmissionResult(
            ticket, confirmed ? null : failure.resolve(ticket.submissionId()));
    if (result.queueUnavailable()) {
      LOG.error("Submission queue is unavailable; the pending submission was finalized");
    }
    return result;
  }
}
