package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.ProblemStatsModels.GradedSubmission;
import com.algoprep.problembank.domain.ports.out.ProblemStatsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplySubmissionGradedCommandHandlerTest {
  private final ProblemStatsRepository stats = mock(ProblemStatsRepository.class);
  private final ApplySubmissionGradedCommandHandler handler =
      new ApplySubmissionGradedCommandHandler(stats);
  private final UUID event = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();

  @Test
  void aRedeliveredEventCountsNothing() {
    when(stats.markConsumed(event)).thenReturn(false);

    assertThat(handler.apply(new GradedSubmission(event, problem, "ACCEPTED"))).isFalse();

    verify(stats, never()).addSubmission(any(), anyBoolean());
  }

  @Test
  void anAcceptedVerdictCountsAsAcceptedAndAnyOtherFinalVerdictDoesNot() {
    when(stats.markConsumed(event)).thenReturn(true);
    when(stats.addSubmission(problem, true)).thenReturn(true);
    when(stats.addSubmission(problem, false)).thenReturn(true);

    assertThat(handler.apply(new GradedSubmission(event, problem, "ACCEPTED"))).isTrue();
    assertThat(handler.apply(new GradedSubmission(event, problem, "WRONG_ANSWER"))).isTrue();
    assertThat(handler.apply(new GradedSubmission(event, problem, "COMPILE_ERROR"))).isTrue();

    verify(stats).addSubmission(problem, true);
    verify(stats, org.mockito.Mockito.times(2)).addSubmission(problem, false);
  }

  @Test
  void aSystemErrorKeepsTheEventIdButCountsNothing() {
    when(stats.markConsumed(event)).thenReturn(true);

    assertThat(handler.apply(new GradedSubmission(event, problem, "SYSTEM_ERROR"))).isTrue();

    verify(stats).markConsumed(event);
    verify(stats, never()).addSubmission(any(), anyBoolean());
  }

  @Test
  void anUnknownProblemIsSkippedWithoutFailingTheDelivery() {
    when(stats.markConsumed(event)).thenReturn(true);
    when(stats.addSubmission(problem, true)).thenReturn(false);

    assertThat(handler.apply(new GradedSubmission(event, problem, "ACCEPTED"))).isTrue();
  }
}
