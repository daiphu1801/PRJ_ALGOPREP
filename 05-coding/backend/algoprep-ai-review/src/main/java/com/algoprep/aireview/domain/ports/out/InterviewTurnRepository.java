package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import java.time.Instant;
import java.util.UUID;

/** Write side of one Mock Interview turn (BR-AI-07 steps 4, 5 and 11); the caller owns the tx. */
public interface InterviewTurnRepository {
  void insertTurn(InterviewTurn line);

  /** Re-uses the pending USER line with the (possibly edited) answer. */
  void updateUserContent(UUID sessionId, int turnIndex, String content);

  /** Sets {@code updated_at} so the idle-session job sees the activity. */
  void touch(UUID sessionId, Instant now);

  /**
   * {@code turn_count + 1}, the new stage and {@code updated_at}, only while the session is
   * running. False when it is not (a job closed it meanwhile).
   */
  boolean advance(UUID sessionId, InterviewSessionStage nextStage, Instant now);
}
