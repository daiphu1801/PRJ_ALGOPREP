package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.CompleteInterviewSessionUseCase;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiNothingToScoreException;
import com.algoprep.aireview.domain.exception.AiSessionBusyException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.ports.out.InterviewSessionLockPort;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * BR-AI-09, trigger USER. Not transactional: the scoring call runs under the session lock and
 * outside any database transaction; {@link InterviewScoreStore} owns the one write. No budget gate
 * and no quota (the learner already paid for the session). A COMPLETED session returns its stored
 * rubric without a model call and without a second event.
 */
@Service
class CompleteInterviewSessionCommandHandler implements CompleteInterviewSessionUseCase {
  private final InterviewSessionRepository sessions;
  private final InterviewSessionLockPort locks;
  private final InterviewScorer scorer;

  CompleteInterviewSessionCommandHandler(
      InterviewSessionRepository sessions, InterviewSessionLockPort locks, InterviewScorer scorer) {
    this.sessions = sessions;
    this.locks = locks;
    this.scorer = scorer;
  }

  @Override
  public InterviewResult complete(UUID callerId, UUID sessionId) {
    InterviewSession first = sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    if (!first.owns(callerId)) {
      throw new AiNotOwnerException();
    }
    InterviewResult done = alreadyDone(first);
    if (done != null) {
      return done;
    }
    return locks
        .withLock(sessionId, () -> underLock(sessionId))
        .orElseThrow(AiSessionBusyException::new);
  }

  private InterviewResult underLock(UUID sessionId) {
    // Re-read under the lock: a turn or the sweep may have finished while this call waited.
    InterviewSession session =
        sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    InterviewResult done = alreadyDone(session);
    if (done != null) {
      return done;
    }
    return scorer.score(session, null, 0).orElseGet(() -> lostRace(sessionId));
  }

  /** The stored result for a COMPLETED session, null for a running one, AIR-111 otherwise. */
  private InterviewResult alreadyDone(InterviewSession session) {
    if (session.stage() == InterviewSessionStage.COMPLETED) {
      return scorer.stored(session).orElseThrow(AiSessionNotRunningException::new);
    }
    if (!session.stage().running()) {
      throw new AiSessionNotRunningException();
    }
    if (session.turnCount() == 0) {
      throw new AiNothingToScoreException();
    }
    return null;
  }

  /** The conditional UPDATE matched nothing: the sweep won. COMPLETED gives its rubric. */
  private InterviewResult lostRace(UUID sessionId) {
    InterviewSession now = sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    if (now.stage() == InterviewSessionStage.COMPLETED) {
      return scorer.stored(now).orElseThrow(AiSessionNotRunningException::new);
    }
    throw new AiSessionNotRunningException();
  }
}
