package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.in.ApplySubmissionAcceptedUseCase;
import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.domain.model.AcceptedSubmission;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD api F1 section 2, {@code SubmissionAccepted}. The event names no class, so the entry is scoped
 * to the class where the student was enrolled and the problem was assigned at {@code occurredAt}.
 *
 * <p>When no class fits, the entry has no class. When problem-bank cannot be asked, the call throws
 * and the transaction rolls back with the processed-event row, so the message is retried: writing
 * an entry with no class would hide the submission from the instructor's feed for good.
 *
 * <p>ponytail: with several matching classes the most recent assignment wins, then the smaller
 * class id. The DD allows one class per entry; show all of them when the feed needs that.
 */
@Service
class ApplySubmissionAcceptedCommandHandler implements ApplySubmissionAcceptedUseCase {
  static final String EVENT_TYPE = "SubmissionAccepted";
  static final String ACTIVITY_TYPE = "SUBMISSION_ACCEPTED";
  static final String REF_TYPE = "SUBMISSION";

  private final ProcessedEventRepository processed;
  private final RecentActivityRepository feed;
  private final ClassAssignmentLookupPort assignments;
  private final IdGeneratorPort ids;

  ApplySubmissionAcceptedCommandHandler(
      ProcessedEventRepository processed,
      RecentActivityRepository feed,
      ClassAssignmentLookupPort assignments,
      IdGeneratorPort ids) {
    this.processed = processed;
    this.feed = feed;
    this.assignments = assignments;
    this.ids = ids;
  }

  @Override
  @Transactional
  public boolean apply(AcceptedSubmission s) {
    if (!processed.markProcessed(s.eventId(), EVENT_TYPE)) {
      return false;
    }
    feed.add(
        new ActivityEntry(
            ids.newId(),
            s.userId(),
            classOf(s),
            ACTIVITY_TYPE,
            REF_TYPE,
            s.submissionId(),
            s.occurredAt()));
    return true;
  }

  private UUID classOf(AcceptedSubmission s) {
    List<UUID> enrolled = feed.classesEnrolledAt(s.userId(), s.occurredAt());
    if (enrolled.isEmpty()) {
      return null;
    }
    return assignments.assignmentsOf(enrolled).stream()
        .filter(w -> w.problemId().equals(s.problemId()) && w.covers(s.occurredAt()))
        .max(
            Comparator.comparing(ClassProblemWindow::assignedAt)
                .thenComparing(ClassProblemWindow::classId, Comparator.reverseOrder()))
        .map(ClassProblemWindow::classId)
        .orElse(null);
  }
}
