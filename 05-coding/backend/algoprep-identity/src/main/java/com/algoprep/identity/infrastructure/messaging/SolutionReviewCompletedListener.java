package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.in.ApplySolutionReviewCompletedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code SolutionReviewCompleted} into the activity feed. Same failure policy as {@link
 * SubmissionAcceptedListener}: a malformed body is dropped, anything else is retried.
 */
@Component("identitySolutionReviewCompletedListener")
class SolutionReviewCompletedListener {
  private static final Logger LOG = LoggerFactory.getLogger(SolutionReviewCompletedListener.class);

  private final ApplySolutionReviewCompletedUseCase useCase;

  SolutionReviewCompletedListener(ApplySolutionReviewCompletedUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = SolutionReviewCompletedBindingConfig.QUEUE)
  void onMessage(byte[] body) {
    try {
      useCase.apply(SolutionReviewCompletedParser.parse(body));
    } catch (IllegalArgumentException invalid) {
      LOG.warn(
          "Dropped a SolutionReviewCompleted message that failed validation: {}",
          invalid.getMessage());
      throw new AmqpRejectAndDontRequeueException(invalid.getMessage());
    }
  }
}
