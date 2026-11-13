package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.in.ApplySubmissionGradedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code SubmissionGraded} (DD api F4 section 25.1) into the F1 read models.
 *
 * <p>ponytail: a malformed body is dropped with a log line and a database failure is requeued with
 * no back-off or dead-letter queue. Add a DLQ and a retry interceptor when the relay is live and a
 * poison message has actually been seen.
 */
@Component
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
