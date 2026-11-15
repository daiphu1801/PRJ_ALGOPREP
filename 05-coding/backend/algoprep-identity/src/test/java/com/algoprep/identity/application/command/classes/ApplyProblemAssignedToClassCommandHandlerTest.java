package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ClassProblemAssignment;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 2, {@code ProblemAssignedToClass}: one class-scoped feed entry per event. */
class ApplyProblemAssignedToClassCommandHandlerTest {
  private static final Instant AT = Instant.parse("2026-11-07T08:00:00Z");

  private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  private final RecentActivityRepository feed = mock(RecentActivityRepository.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ApplyProblemAssignedToClassCommandHandler handler =
      new ApplyProblemAssignedToClassCommandHandler(processed, feed, ids);
  private final UUID entryId = UUID.randomUUID();
  private final ClassProblemAssignment event =
      new ClassProblemAssignment(
          UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AT);

  @Test
  void writesOneEntryScopedToTheClassAndAttributedToTheInstructor() {
    when(processed.markProcessed(event.eventId(), "ProblemAssignedToClass")).thenReturn(true);
    when(ids.newId()).thenReturn(entryId);
    when(feed.addIfClassExists(any())).thenReturn(true);

    assertThat(handler.apply(event)).isTrue();

    verify(feed)
        .addIfClassExists(
            new ActivityEntry(
                entryId,
                event.actorUserId(),
                event.classId(),
                "CLASS_PROBLEM_PUBLISHED",
                "PROBLEM",
                event.problemId(),
                AT));
  }

  @Test
  void aRedeliveryWritesNothing() {
    when(processed.markProcessed(event.eventId(), "ProblemAssignedToClass")).thenReturn(false);

    assertThat(handler.apply(event)).isFalse();

    verify(feed, never()).addIfClassExists(any());
  }

  @Test
  void anEventForADeletedClassIsConsumedWithoutAnEntry() {
    when(processed.markProcessed(event.eventId(), "ProblemAssignedToClass")).thenReturn(true);
    when(ids.newId()).thenReturn(entryId);
    when(feed.addIfClassExists(any())).thenReturn(false);

    assertThat(handler.apply(event)).isFalse();
  }
}
