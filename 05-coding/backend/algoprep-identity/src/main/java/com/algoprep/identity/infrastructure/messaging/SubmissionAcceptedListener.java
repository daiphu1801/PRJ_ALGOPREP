package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.in.ApplySubmissionAcceptedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code SubmissionAccepted} (DD api F4 section 25.3) into the activity feed. Same failure
 * policy as {@link SubmissionGradedListener}: a malformed body is dropped, anything else is
 * retried.
 */
@Component
class SubmissionAcceptedListener {
  private static final Logger LOG = LoggerFactory.getLogger(SubmissionAcceptedListener.class);

  private final ApplySubmissionAcceptedUseCase useCase;

  SubmissionAcceptedListener(ApplySubmissionAcceptedUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = SubmissionGradedBindingConfig.ACCEPTED_QUEUE)
  void onMessage(byte[] body) {
    try {
      useCase.apply(SubmissionAcceptedParser.parse(body));
    } catch (IllegalArgumentException invalid) {
      LOG.warn(
          "Dropped a SubmissionAccepted message that failed validation: {}", invalid.getMessage());
      throw new AmqpRejectAndDontRequeueException(invalid.getMessage());
    }
  }
}
