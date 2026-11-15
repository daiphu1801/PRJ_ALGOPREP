package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.in.ApplyProblemAssignedToClassUseCase;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ClassProblemAssignment;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD api F1 section 2, {@code ProblemAssignedToClass}: one {@code CLASS_PROBLEM_PUBLISHED} entry,
 * scoped to the class and attributed to the instructor who assigned the problem. The processed
 * event row and the feed row commit together, so a redelivery adds nothing.
 *
 * <p>ponytail: an event for a class deleted in the meantime is marked processed and writes no row,
 * because the feed is scoped to a class and no instructor can read an entry of a class that is
 * gone; a retry could never succeed.
 */
@Service
class ApplyProblemAssignedToClassCommandHandler implements ApplyProblemAssignedToClassUseCase {
  static final String EVENT_TYPE = "ProblemAssignedToClass";
  static final String ACTIVITY_TYPE = "CLASS_PROBLEM_PUBLISHED";
  static final String REF_TYPE = "PROBLEM";

  private final ProcessedEventRepository processed;
  private final RecentActivityRepository feed;
  private final IdGeneratorPort ids;

  ApplyProblemAssignedToClassCommandHandler(
      ProcessedEventRepository processed, RecentActivityRepository feed, IdGeneratorPort ids) {
    this.processed = processed;
    this.feed = feed;
    this.ids = ids;
  }

  @Override
  @Transactional
  public boolean apply(ClassProblemAssignment a) {
    if (!processed.markProcessed(a.eventId(), EVENT_TYPE)) {
      return false;
    }
    return feed.addIfClassExists(
        new ActivityEntry(
            ids.newId(),
            a.actorUserId(),
            a.classId(),
            ACTIVITY_TYPE,
            REF_TYPE,
            a.problemId(),
            a.occurredAt()));
  }
}
