package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.in.ApplyMockInterviewFinishedUseCase;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.FinishedMockInterview;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD api F1 section 2, {@code MockInterviewFinished}: one {@code MOCK_INTERVIEW_FINISHED} entry in
 * the personal feed of the learner (no class scope). The processed event row and the feed row
 * commit together, so a redelivery adds nothing.
 */
@Service
class ApplyMockInterviewFinishedCommandHandler implements ApplyMockInterviewFinishedUseCase {
  static final String EVENT_TYPE = "MockInterviewFinished";
  static final String ACTIVITY_TYPE = "MOCK_INTERVIEW_FINISHED";
  static final String REF_TYPE = "INTERVIEW_SESSION";

  private final ProcessedEventRepository processed;
  private final RecentActivityRepository feed;
  private final IdGeneratorPort ids;

  ApplyMockInterviewFinishedCommandHandler(
      ProcessedEventRepository processed, RecentActivityRepository feed, IdGeneratorPort ids) {
    this.processed = processed;
    this.feed = feed;
    this.ids = ids;
  }

  @Override
  @Transactional
  public boolean apply(FinishedMockInterview r) {
    if (!processed.markProcessed(r.eventId(), EVENT_TYPE)) {
      return false;
    }
    feed.add(
        new ActivityEntry(
            ids.newId(), r.userId(), null, ACTIVITY_TYPE, REF_TYPE, r.sessionId(), r.occurredAt()));
    return true;
  }
}
