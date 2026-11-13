package com.algoprep.problembank.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.problembank.application.ports.in.ApplySubmissionGradedUseCase;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class SubmissionGradedMessagingTest {
  private final UUID event = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();

  private byte[] body(String type, String verdict, String problemId) {
    return ("{\"eventId\":\""
            + event
            + "\",\"eventType\":\""
            + type
            + "\",\"payload\":{\"problemId\":\""
            + problemId
            + "\",\"verdict\":\""
            + verdict
            + "\",\"userId\":\"ignored\"}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void theParserKeepsEventProblemAndVerdictOnly() {
    var parsed =
        SubmissionGradedParser.parse(body("SubmissionGraded", "ACCEPTED", problem.toString()));

    assertThat(parsed.eventId()).isEqualTo(event);
    assertThat(parsed.problemId()).isEqualTo(problem);
    assertThat(parsed.verdict()).isEqualTo("ACCEPTED");
  }

  @Test
  void theParserRejectsAWrongTypeAVerdictOutsideTheSetAndABrokenId() {
    for (var bad :
        new byte[][] {
          body("SubmissionAccepted", "ACCEPTED", problem.toString()),
          body("SubmissionGraded", "MAYBE", problem.toString()),
          body("SubmissionGraded", "ACCEPTED", "not-a-uuid"),
          "not json".getBytes(StandardCharsets.UTF_8)
        }) {
      assertThatThrownBy(() -> SubmissionGradedParser.parse(bad))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void theListenerAppliesAGoodMessageAndRejectsABadOneWithoutRequeue() {
    var useCase = mock(ApplySubmissionGradedUseCase.class);
    var listener = new SubmissionGradedListener(useCase);

    listener.onMessage(body("SubmissionGraded", "WRONG_ANSWER", problem.toString()));
    verify(useCase).apply(any());

    var bad = "{}".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> listener.onMessage(bad))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void theBindingUsesTheOwnQueueAndTheJudgeRoutingKey() {
    var config = new SubmissionGradedBindingConfig();
    var queue = config.problemSubmissionGradedQueue();
    var binding =
        config.problemSubmissionGradedBinding(queue, config.problemDomainEventsExchange());

    assertThat(queue.getName()).isEqualTo("problem-bank.submission-graded");
    assertThat(queue.isDurable()).isTrue();
    assertThat(binding.getRoutingKey()).isEqualTo("judge.submission.graded");
    assertThat(binding.getExchange()).isEqualTo("algoprep.domain-events");
  }
}
