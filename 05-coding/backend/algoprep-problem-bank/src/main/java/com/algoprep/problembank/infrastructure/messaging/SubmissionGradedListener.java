package com.algoprep.problembank.infrastructure.messaging;

import com.algoprep.problembank.application.ports.in.ApplySubmissionGradedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code SubmissionGraded} (DD F2 section 59.2) into {@code problem_stats}.
 *
 * <p>ponytail: a malformed body is dropped with a log line and a database failure is requeued with
 * no back-off or dead-letter queue, as in the identity consumer. Add a DLQ and a retry interceptor
 * when a poison message has actually been seen.
 */
@Component("problemSubmissionGradedListener")
class SubmissionGradedListener {
  private static final Logger LOG = LoggerFactory.getLogger(SubmissionGradedListener.class);

  private final ApplySubmissionGradedUseCase useCase;

  SubmissionGradedListener(ApplySubmissionGradedUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = SubmissionGradedBindingConfig.QUEUE)
  void onMessage(byte[] body) {
    try {
      useCase.apply(SubmissionGradedParser.parse(body));
    } catch (IllegalArgumentException invalid) {
      LOG.warn(
          "Dropped a SubmissionGraded message that failed validation: {}", invalid.getMessage());
      throw new AmqpRejectAndDontRequeueException(invalid.getMessage());
    }
  }
}
