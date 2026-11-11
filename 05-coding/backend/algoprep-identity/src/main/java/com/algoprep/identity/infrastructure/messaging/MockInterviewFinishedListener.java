package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.in.ApplyMockInterviewFinishedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code MockInterviewFinished} into the activity feed. Same failure policy as {@link
 * SolutionReviewCompletedListener}: a malformed body is dropped, anything else is retried.
 */
@Component("identityMockInterviewFinishedListener")
class MockInterviewFinishedListener {
  private static final Logger LOG = LoggerFactory.getLogger(MockInterviewFinishedListener.class);

  private final ApplyMockInterviewFinishedUseCase useCase;

  MockInterviewFinishedListener(ApplyMockInterviewFinishedUseCase useCase) {
    this.useCase = useCase;
  }

  @RabbitListener(queues = MockInterviewFinishedBindingConfig.QUEUE)
  void onMessage(byte[] body) {
    try {
      useCase.apply(MockInterviewFinishedParser.parse(body));
    } catch (IllegalArgumentException invalid) {
      LOG.warn(
          "Dropped a MockInterviewFinished message that failed validation: {}",
          invalid.getMessage());
      throw new AmqpRejectAndDontRequeueException(invalid.getMessage());
    }
  }
}
