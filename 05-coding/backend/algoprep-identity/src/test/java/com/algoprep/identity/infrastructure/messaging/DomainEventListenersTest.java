package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.identity.application.ports.in.classes.ApplyProblemAssignedToClassUseCase;
import com.algoprep.identity.application.ports.in.progress.ApplyMockInterviewFinishedUseCase;
import com.algoprep.identity.application.ports.in.progress.ApplySolutionReviewCompletedUseCase;
import com.algoprep.identity.application.ports.in.progress.ApplySubmissionAcceptedUseCase;
import com.algoprep.identity.application.ports.in.progress.ApplySubmissionGradedUseCase;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Inbound domain events: a valid envelope reaches the use case, an invalid one is dead-lettered.
 */
class DomainEventListenersTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String USER = "22222222-2222-2222-2222-222222222222";
  private static final String OTHER = "33333333-3333-3333-3333-333333333333";
  private static final byte[] GARBAGE = "not json".getBytes(StandardCharsets.UTF_8);

  private static byte[] envelope(String type, String payload) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\"2026-11-10T08:00:00Z\",\"schemaVersion\":1,\"payload\":"
            + payload
            + "}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void submissionGradedValidAndInvalid() {
    ApplySubmissionGradedUseCase uc = mock(ApplySubmissionGradedUseCase.class);
    var listener = new SubmissionGradedListener(uc);
    listener.onMessage(
        envelope(
            "SubmissionGraded",
            "{\"userId\":\""
                + USER
                + "\",\"problemId\":\""
                + OTHER
                + "\",\"verdict\":\"ACCEPTED\",\"language\":\"JAVA\",\"passedTestcaseRatio\":1,"
                + "\"submittedAt\":\"2026-11-10T08:00:00Z\",\"firstAttemptForProblem\":true}"));
    verify(uc).apply(any());
    assertThatThrownBy(() -> listener.onMessage(GARBAGE))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void submissionAcceptedValidAndInvalid() {
    ApplySubmissionAcceptedUseCase uc = mock(ApplySubmissionAcceptedUseCase.class);
    var listener = new SubmissionAcceptedListener(uc);
    listener.onMessage(
        envelope(
            "SubmissionAccepted",
            "{\"submissionId\":\""
                + OTHER
                + "\",\"userId\":\""
                + USER
                + "\",\"problemId\":\""
                + OTHER
                + "\",\"language\":\"JAVA\"}"));
    verify(uc).apply(any());
    assertThatThrownBy(() -> listener.onMessage(GARBAGE))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void problemAssignedValidAndInvalid() {
    ApplyProblemAssignedToClassUseCase uc = mock(ApplyProblemAssignedToClassUseCase.class);
    var listener = new ProblemAssignedListener(uc);
    listener.onMessage(
        envelope(
            "ProblemAssignedToClass",
            "{\"problemId\":\""
                + OTHER
                + "\",\"classId\":\""
                + USER
                + "\",\"actorUserId\":\""
                + USER
                + "\",\"problemTitle\":\"Two Sum\",\"assignedAt\":\"2026-11-07T08:00:00Z\"}"));
    verify(uc).apply(any());
    assertThatThrownBy(() -> listener.onMessage(GARBAGE))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void solutionReviewCompletedValidAndInvalid() {
    ApplySolutionReviewCompletedUseCase uc = mock(ApplySolutionReviewCompletedUseCase.class);
    var listener = new SolutionReviewCompletedListener(uc);
    listener.onMessage(
        envelope(
            "SolutionReviewCompleted",
            "{\"reviewId\":\""
                + OTHER
                + "\",\"submissionId\":\""
                + OTHER
                + "\",\"userId\":\""
                + USER
                + "\",\"problemId\":\""
                + OTHER
                + "\",\"readabilityScore\":4}"));
    verify(uc).apply(any());
    assertThatThrownBy(() -> listener.onMessage(GARBAGE))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void mockInterviewFinishedValidAndInvalid() {
    ApplyMockInterviewFinishedUseCase uc = mock(ApplyMockInterviewFinishedUseCase.class);
    var listener = new MockInterviewFinishedListener(uc);
    listener.onMessage(
        envelope(
            "MockInterviewFinished",
            "{\"sessionId\":\""
                + OTHER
                + "\",\"userId\":\""
                + USER
                + "\",\"entryType\":\"TOPIC_SELECTION\",\"submissionId\":null,\"overallScore\":6.5,"
                + "\"rubric\":{\"CLARITY\":8}}"));
    verify(uc).apply(any());
    assertThatThrownBy(() -> listener.onMessage(GARBAGE))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
  }

  @Test
  void wrongEventTypeIsRejectedWithoutTouchingTheUseCase() {
    ApplySubmissionGradedUseCase uc = mock(ApplySubmissionGradedUseCase.class);
    var listener = new SubmissionGradedListener(uc);
    assertThatThrownBy(() -> listener.onMessage(envelope("Other", "{}")))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    verifyNoInteractions(uc);
  }

  @Test
  void upstreamBindingsUseTheAgreedRoutingKeys() {
    var exchange = new SubmissionGradedBindingConfig().domainEventsExchange();
    var assigned = new ProblemAssignedBindingConfig();
    var review = new SolutionReviewCompletedBindingConfig();
    var interview = new MockInterviewFinishedBindingConfig();
    assertThat(assigned.identityProblemAssignedQueue().getName())
        .isEqualTo("identity.problem-assigned");
    assertThat(
            assigned
                .identityProblemAssignedBinding(assigned.identityProblemAssignedQueue(), exchange)
                .getRoutingKey())
        .isEqualTo("problem.assignment.assigned");
    assertThat(
            review
                .identitySolutionReviewCompletedBinding(
                    review.identitySolutionReviewCompletedQueue(), exchange)
                .getRoutingKey())
        .isEqualTo("ai.solution-review.completed");
    assertThat(
            interview
                .identityMockInterviewFinishedBinding(
                    interview.identityMockInterviewFinishedQueue(), exchange)
                .getRoutingKey())
        .isEqualTo("ai.interview-session.finished");
  }

  @Test
  void bindingConfigsDeclareDurableQueuesBoundToTheDomainExchange() {
    var graded = new SubmissionGradedBindingConfig();
    var exchange = graded.domainEventsExchange();
    assertThat(exchange.getName()).isEqualTo(SubmissionGradedBindingConfig.EXCHANGE);
    assertThat(graded.submissionGradedQueue().isDurable()).isTrue();
    assertThat(graded.submissionAcceptedQueue().getName())
        .isEqualTo("identity.submission-accepted");
    assertThat(
            graded
                .submissionGradedBinding(graded.submissionGradedQueue(), exchange)
                .getRoutingKey())
        .isEqualTo("judge.submission.graded");
    assertThat(
            graded
                .submissionAcceptedBinding(graded.submissionAcceptedQueue(), exchange)
                .getRoutingKey())
        .isEqualTo("judge.submission.accepted");
    assertThat(new ClassEventExchangeConfig().identityEventsExchange().getName())
        .isEqualTo("algoprep.identity.events");
  }
}
