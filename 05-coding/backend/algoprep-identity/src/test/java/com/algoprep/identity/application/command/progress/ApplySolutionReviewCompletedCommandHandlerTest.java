package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.CompletedSolutionReview;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 2, {@code SolutionReviewCompleted}: one personal feed entry per event. */
class ApplySolutionReviewCompletedCommandHandlerTest {
  private static final Instant AT = Instant.parse("2026-11-10T08:00:00Z");

  private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  private final RecentActivityRepository feed = mock(RecentActivityRepository.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ApplySolutionReviewCompletedCommandHandler handler =
      new ApplySolutionReviewCompletedCommandHandler(processed, feed, ids);
  private final UUID entryId = UUID.randomUUID();
  private final CompletedSolutionReview event =
      new CompletedSolutionReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AT);

  @Test
  void writesOneUnscopedEntryForTheLearnerPointingAtTheReview() {
    when(processed.markProcessed(event.eventId(), "SolutionReviewCompleted")).thenReturn(true);
    when(ids.newId()).thenReturn(entryId);

    assertThat(handler.apply(event)).isTrue();

    verify(feed)
        .add(
            new ActivityEntry(
                entryId,
                event.userId(),
                null,
                "SOLUTION_REVIEW_COMPLETED",
                "SOLUTION_REVIEW",
                event.reviewId(),
                AT));
  }

  @Test
  void aRedeliveryWritesNothing() {
    when(processed.markProcessed(event.eventId(), "SolutionReviewCompleted")).thenReturn(false);

    assertThat(handler.apply(event)).isFalse();

    verify(feed, never()).add(any());
  }
}
