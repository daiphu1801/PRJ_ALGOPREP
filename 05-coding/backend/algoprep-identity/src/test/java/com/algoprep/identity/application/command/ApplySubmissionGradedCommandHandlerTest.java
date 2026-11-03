package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.GradedSubmission;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.SubmissionStatsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 2: what one {@code SubmissionGraded} event does to the read models. */
class ApplySubmissionGradedCommandHandlerTest {
  private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  private final SubmissionStatsRepository stats = mock(SubmissionStatsRepository.class);
  private final ApplySubmissionGradedCommandHandler handler =
      new ApplySubmissionGradedCommandHandler(processed, stats);
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();

  private GradedSubmission event(String verdict, BigDecimal ratio, Instant submittedAt) {
    return new GradedSubmission(
        UUID.randomUUID(), user, problem, verdict, ratio, "PYTHON", submittedAt, true);
  }

  @Test
  void aRedeliveredEventChangesNothing() {
    GradedSubmission e = event("ACCEPTED", BigDecimal.ONE, Instant.parse("2026-11-03T05:00:00Z"));
    when(processed.markProcessed(e.eventId(), "SubmissionGraded")).thenReturn(false);

    assertThat(handler.apply(e)).isFalse();

    verify(stats, never()).keepBestScore(any(), any(), any(), any());
    verify(stats, never()).addToTotals(any());
    verify(stats, never()).addToDay(any(), any());
  }

  @Test
  void aFirstDeliveryUpdatesBestScoreTotalsAndDay() {
    GradedSubmission e =
        event("WRONG_ANSWER", new BigDecimal("0.6667"), Instant.parse("2026-11-03T05:00:00Z"));
    when(processed.markProcessed(e.eventId(), "SubmissionGraded")).thenReturn(true);

    assertThat(handler.apply(e)).isTrue();

    verify(stats).keepBestScore(user, problem, new BigDecimal("0.6667"), "WRONG_ANSWER");
    verify(stats).addToTotals(e);
    verify(stats).addToDay(user, LocalDate.of(2026, 11, 3));
  }

  @Test
  void aSubmissionAfterMidnightInVietnamBelongsToTheNextDay() {
    // 17:30 UTC on the 3rd is 00:30 on the 4th at UTC+7; 16:59 UTC is still the 3rd.
    GradedSubmission late =
        event("ACCEPTED", BigDecimal.ONE, Instant.parse("2026-11-03T17:30:00Z"));
    GradedSubmission early =
        event("ACCEPTED", BigDecimal.ONE, Instant.parse("2026-11-03T16:59:59Z"));
    when(processed.markProcessed(any(), eq("SubmissionGraded"))).thenReturn(true);

    handler.apply(late);
    handler.apply(early);

    verify(stats).addToDay(user, LocalDate.of(2026, 11, 4));
    verify(stats).addToDay(user, LocalDate.of(2026, 11, 3));
  }

  @Test
  void aCompileErrorCountsAsZeroForTheBestScore() {
    GradedSubmission e = event("COMPILE_ERROR", null, Instant.parse("2026-11-03T05:00:00Z"));
    when(processed.markProcessed(e.eventId(), "SubmissionGraded")).thenReturn(true);

    handler.apply(e);

    verify(stats).keepBestScore(user, problem, BigDecimal.ZERO, "COMPILE_ERROR");
  }

  @Test
  void aSystemErrorIsCountedButNeverBecomesABestScore() {
    GradedSubmission e = event("SYSTEM_ERROR", null, Instant.parse("2026-11-03T05:00:00Z"));
    when(processed.markProcessed(e.eventId(), "SubmissionGraded")).thenReturn(true);

    handler.apply(e);

    verify(stats, never()).keepBestScore(any(), any(), any(), any());
    verify(stats).addToTotals(e);
    verify(stats).addToDay(user, LocalDate.of(2026, 11, 3));
  }
}
