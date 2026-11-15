package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.FinishedMockInterview;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 2, {@code MockInterviewFinished}: one personal feed entry per event. */
class ApplyMockInterviewFinishedCommandHandlerTest {
  private static final Instant AT = Instant.parse("2026-11-10T08:00:00Z");

  private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  private final RecentActivityRepository feed = mock(RecentActivityRepository.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ApplyMockInterviewFinishedCommandHandler handler =
      new ApplyMockInterviewFinishedCommandHandler(processed, feed, ids);
  private final UUID entryId = UUID.randomUUID();
  private final FinishedMockInterview event =
      new FinishedMockInterview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AT);

  @Test
  void writesOneUnscopedEntryForTheLearnerPointingAtTheSession() {
    when(processed.markProcessed(event.eventId(), "MockInterviewFinished")).thenReturn(true);
    when(ids.newId()).thenReturn(entryId);

    assertThat(handler.apply(event)).isTrue();

    verify(feed)
        .add(
            new ActivityEntry(
                entryId,
                event.userId(),
                null,
                "MOCK_INTERVIEW_FINISHED",
                "INTERVIEW_SESSION",
                event.sessionId(),
                AT));
  }

  @Test
  void aRedeliveryWritesNothing() {
    when(processed.markProcessed(event.eventId(), "MockInterviewFinished")).thenReturn(false);

    assertThat(handler.apply(event)).isFalse();

    verify(feed, never()).add(any());
  }
}
