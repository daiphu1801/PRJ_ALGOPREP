package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.SweepOutcome;
import com.algoprep.aireview.application.ports.in.SweepIdleInterviewSessionsUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.IdleSession;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.aireview.domain.ports.out.InterviewSessionLockPort;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * JOB-AI-04. Candidates are only a list: each one is re-checked under the session lock (still
 * running, still idle) and every write is guarded by the observed {@code updated_at}, so a learner
 * who came back, or a racing complete, always wins. A session with at least {@code enoughTurns}
 * turns is scored through the same {@link InterviewScorer} as the learner's own complete; a shorter
 * one is abandoned with no score and no event. A scoring failure caused by the LLM leaves the
 * session for the next run and abandons it after {@code maxScoreRetries} failures. No budget gate:
 * the learner already paid for the session. Not transactional; a failure on one session never stops
 * the others.
 */
@Service
class SweepIdleInterviewSessionsCommandHandler implements SweepIdleInterviewSessionsUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(SweepIdleInterviewSessionsCommandHandler.class);

  private final InterviewScoringRepository scoring;
  private final InterviewSessionRepository sessions;
  private final InterviewSessionLockPort locks;
  private final InterviewScorer scorer;
  private final SweepCollaborators with;

  SweepIdleInterviewSessionsCommandHandler(
      InterviewScoringRepository scoring,
      InterviewSessionRepository sessions,
      InterviewSessionLockPort locks,
      InterviewScorer scorer,
      SweepCollaborators with) {
    this.scoring = scoring;
    this.sessions = sessions;
    this.locks = locks;
    this.scorer = scorer;
    this.with = with;
  }

  private enum Result {
    SCORED,
    ABANDONED,
    SKIPPED,
    FAILED
  }

  @Override
  public SweepOutcome sweep() {
    int[] counts = new int[Result.values().length];
    List<IdleSession> candidates =
        scoring.idleSessions(with.limits().idleMinutes(), with.limits().batchSize());
    for (IdleSession candidate : candidates) {
      counts[handle(candidate).ordinal()]++;
    }
    return new SweepOutcome(
        counts[Result.SCORED.ordinal()],
        counts[Result.ABANDONED.ordinal()],
        counts[Result.SKIPPED.ordinal()],
        counts[Result.FAILED.ordinal()]);
  }

  private Result handle(IdleSession candidate) {
    try {
      return locks.withLock(candidate.id(), () -> underLock(candidate)).orElse(Result.SKIPPED);
    } catch (RuntimeException failure) {
      LOG.warn("JOB-AI-04 failed for one session; the next run retries", failure);
      return Result.FAILED;
    }
  }

  private Result underLock(IdleSession candidate) {
    Optional<IdleSession> fresh = scoring.idleSession(candidate.id(), with.limits().idleMinutes());
    Optional<InterviewSession> session = sessions.find(candidate.id());
    if (fresh.isEmpty() || session.isEmpty() || !session.get().stage().running()) {
      return Result.SKIPPED;
    }
    IdleSession idle = fresh.get();
    if (idle.turnCount() < with.limits().enoughTurns()) {
      return abandon(idle) ? Result.ABANDONED : Result.SKIPPED;
    }
    try {
      Optional<?> done = scorer.score(session.get(), idle.updatedAt(), with.limits().idleMinutes());
      with.retries().clear(idle.id());
      return done.isPresent() ? Result.SCORED : Result.SKIPPED;
    } catch (AiProviderUnavailableException | AiInvalidResponseException failure) {
      return afterLlmFailure(idle, failure);
    }
  }

  private Result afterLlmFailure(IdleSession idle, RuntimeException failure) {
    int failures = with.retries().recordFailure(idle.id());
    if (failures < with.limits().maxScoreRetries()) {
      LOG.warn(
          "JOB-AI-04 scoring failed ({} of {}); retrying next run",
          failures,
          with.limits().maxScoreRetries(),
          failure);
      return Result.FAILED;
    }
    LOG.warn("JOB-AI-04 scoring failed {} times; abandoning the session", failures, failure);
    with.retries().clear(idle.id());
    return abandon(idle) ? Result.ABANDONED : Result.SKIPPED;
  }

  private boolean abandon(IdleSession idle) {
    return scoring.markAbandoned(
        idle.id(), with.clock().now(), idle.updatedAt(), with.limits().idleMinutes());
  }
}
