package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.IdleSession;
import com.algoprep.aireview.domain.model.RubricRow;
import com.algoprep.aireview.domain.model.RubricScore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Rubric rows and the terminal state changes of {@code ai.interview_sessions} (BR-AI-09,
 * JOB-AI-04). Every state change is conditional on the session still running, so a racing writer
 * wins once.
 */
public interface InterviewScoringRepository {
  /** The stored rows of the session in the fixed criterion order (fewer than four if partial). */
  List<RubricScore> rubric(UUID sessionId);

  /**
   * {@code stage = COMPLETED, ended_at} while the session is running. With {@code
   * observedUpdatedAt} (sweep) the row must also be unchanged and idle for {@code idleMinutes}.
   * False when it matched nothing.
   */
  boolean markCompleted(UUID sessionId, Instant now, Instant observedUpdatedAt, int idleMinutes);

  /** Inserts the rows of one rubric; the unique key makes a second set fail. */
  void insertRubric(UUID sessionId, List<RubricScore> scores, Instant now, Supplier<UUID> ids);

  /** {@code stage = ABANDONED, ended_at} under the same guard as the sweep branch above. */
  boolean markAbandoned(UUID sessionId, Instant now, Instant observedUpdatedAt, int idleMinutes);

  /** Running sessions idle for {@code idleMinutes}, oldest first. */
  List<IdleSession> idleSessions(int idleMinutes, int limit);

  /** The same test for one session, used to re-check a candidate under the session lock. */
  Optional<IdleSession> idleSession(UUID sessionId, int idleMinutes);

  /** Rubric rows of the user's COMPLETED sessions that ended at or after {@code from}. */
  List<RubricRow> completedRubrics(UUID userId, Instant from);
}
