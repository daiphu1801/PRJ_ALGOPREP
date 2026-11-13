package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.in.ApplyProblemAssignedToClassUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code ProblemAssignedToClass} into the activity feed. Same failure policy as {@link
 * SubmissionAcceptedListener}: a malformed body is dropped, anything else is retried.
 */
@Component("identityProblemAssignedListener")
class ProblemAssignedListener {
  private static final Logger LOG = LoggerFactory.getLogger(ProblemAssignedListener.class);

  private final ApplyProblemAssignedToClassUseCase useCase;

  ProblemAssignedListener(ApplyProblemAssignedToClassUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = ProblemAssignedBindingConfig.QUEUE)
  void onMessage(byte[] body) {
    try {
      useCase.apply(ProblemAssignedParser.parse(body));
    } catch (IllegalArgumentException invalid) {
      LOG.warn(
          "Dropped a ProblemAssignedToClass message that failed validation: {}",
          invalid.getMessage());
      throw new AmqpRejectAndDontRequeueException(invalid.getMessage());
    }
  }
}
