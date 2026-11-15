package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.domain.model.AcceptedSubmission;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 2, {@code SubmissionAccepted}: which class an activity entry belongs to. */
class ApplySubmissionAcceptedCommandHandlerTest {
  private static final Instant AT = Instant.parse("2026-11-03T10:00:00Z");
  private static final UUID CLASS_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID CLASS_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

  private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  private final RecentActivityRepository feed = mock(RecentActivityRepository.class);
  private final ClassAssignmentLookupPort assignments = mock(ClassAssignmentLookupPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ApplySubmissionAcceptedCommandHandler handler =
      new ApplySubmissionAcceptedCommandHandler(processed, feed, assignments, ids);
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID entryId = UUID.randomUUID();
  private final AcceptedSubmission event =
      new AcceptedSubmission(UUID.randomUUID(), user, problem, UUID.randomUUID(), AT);

  ApplySubmissionAcceptedCommandHandlerTest() {
    when(processed.markProcessed(event.eventId(), "SubmissionAccepted")).thenReturn(true);
    when(ids.newId()).thenReturn(entryId);
  }

  private ClassProblemWindow window(UUID classId, UUID forProblem, String from, String to) {
    return new ClassProblemWindow(
        classId, forProblem, Instant.parse(from), to == null ? null : Instant.parse(to));
  }

  private void added(UUID expectedClass) {
    verify(feed)
        .add(
            new ActivityEntry(
                entryId,
                user,
                expectedClass,
                "SUBMISSION_ACCEPTED",
                "SUBMISSION",
                event.submissionId(),
                AT));
  }

  @Test
  void aRedeliveredEventAddsNothing() {
    when(processed.markProcessed(event.eventId(), "SubmissionAccepted")).thenReturn(false);

    assertThat(handler.apply(event)).isFalse();

    verify(feed, never()).add(any());
  }

  @Test
  void aStudentInNoClassGetsAnEntryWithoutAClassAndNoLookup() {
    when(feed.classesEnrolledAt(user, AT)).thenReturn(List.of());

    assertThat(handler.apply(event)).isTrue();

    added(null);
    verify(assignments, never()).assignmentsOf(anyCollection());
  }

  @Test
  void theEntryTakesTheClassWhereTheProblemWasAssignedAtThatMoment() {
    when(feed.classesEnrolledAt(user, AT)).thenReturn(List.of(CLASS_A, CLASS_B));
    when(assignments.assignmentsOf(List.of(CLASS_A, CLASS_B)))
        .thenReturn(
            List.of(
                window(CLASS_A, UUID.randomUUID(), "2026-11-01T00:00:00Z", null),
                window(CLASS_B, problem, "2026-11-01T00:00:00Z", null)));

    handler.apply(event);

    added(CLASS_B);
  }

  @Test
  void anAssignmentThatEndedOrNotYetStartedDoesNotCount() {
    when(feed.classesEnrolledAt(user, AT)).thenReturn(List.of(CLASS_A, CLASS_B));
    when(assignments.assignmentsOf(List.of(CLASS_A, CLASS_B)))
        .thenReturn(
            List.of(
                window(CLASS_A, problem, "2026-10-01T00:00:00Z", "2026-11-03T10:00:00Z"),
                window(CLASS_B, problem, "2026-11-03T10:00:01Z", null)));

    handler.apply(event);

    added(null);
  }

  @Test
  void theMostRecentAssignmentWinsAndTheSmallerClassIdBreaksATie() {
    when(feed.classesEnrolledAt(user, AT)).thenReturn(List.of(CLASS_A, CLASS_B));
    when(assignments.assignmentsOf(List.of(CLASS_A, CLASS_B)))
        .thenReturn(
            List.of(
                window(CLASS_A, problem, "2026-11-01T00:00:00Z", null),
                window(CLASS_B, problem, "2026-11-02T00:00:00Z", null)));
    handler.apply(event);
    added(CLASS_B);

    AcceptedSubmission tied =
        new AcceptedSubmission(UUID.randomUUID(), user, problem, UUID.randomUUID(), AT);
    when(processed.markProcessed(tied.eventId(), "SubmissionAccepted")).thenReturn(true);
    when(assignments.assignmentsOf(List.of(CLASS_A, CLASS_B)))
        .thenReturn(
            List.of(
                window(CLASS_B, problem, "2026-11-02T00:00:00Z", null),
                window(CLASS_A, problem, "2026-11-02T00:00:00Z", null)));

    handler.apply(tied);

    verify(feed)
        .add(
            new ActivityEntry(
                entryId,
                user,
                CLASS_A,
                "SUBMISSION_ACCEPTED",
                "SUBMISSION",
                tied.submissionId(),
                AT));
  }

  @Test
  void whenProblemBankCannotBeAskedNothingIsWrittenSoTheMessageCanBeRetried() {
    when(feed.classesEnrolledAt(user, AT)).thenReturn(List.of(CLASS_A));
    when(assignments.assignmentsOf(List.of(CLASS_A)))
        .thenThrow(new IllegalStateException("unavailable"));

    assertThatThrownBy(() -> handler.apply(event)).isInstanceOf(IllegalStateException.class);

    verify(feed, never()).add(any());
  }
}
