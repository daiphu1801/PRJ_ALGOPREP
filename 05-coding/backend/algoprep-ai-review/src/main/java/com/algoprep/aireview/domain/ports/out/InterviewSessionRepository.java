package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tables {@code ai.interview_sessions} and {@code ai.interview_turns}. */
public interface InterviewSessionRepository {
  /** Session row plus its opening AI line; the caller wraps both calls in one transaction. */
  void insertOpened(InterviewSession session, InterviewTurn opening);

  Optional<InterviewSession> find(UUID sessionId);

  /** All lines of the session ordered by {@code turn_index}. */
  List<InterviewTurn> turns(UUID sessionId);

  /** Every session of the user, oldest {@code started_at} first (P4). */
  List<InterviewSession> findAllByUser(UUID userId);

  /** Every line of every session of the user, by session then {@code turn_index} (P4). */
  List<InterviewTurn> turnsOfUser(UUID userId);

  /** Number of USER lines written while the session was in {@code stage}. */
  int countUserTurns(UUID sessionId, InterviewStage stage);

  /**
   * Adds one hint while the session is running and {@code hint_count < maxHints}; false when the
   * guard refused (the row changed under the caller).
   */
  boolean incrementHintCount(UUID sessionId, int maxHints);
}
