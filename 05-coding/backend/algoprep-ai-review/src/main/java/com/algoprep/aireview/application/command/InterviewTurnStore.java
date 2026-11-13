package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.ports.out.InterviewTurnRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The two short transactions of a turn (BR-AI-07): the USER line before the LLM call, and the AI
 * line plus counters after it. The LLM call itself never runs inside either.
 */
@Service
class InterviewTurnStore {
  private final InterviewTurnRepository turns;
  private final IdGeneratorPort ids;
  private final ClockPort clock;

  InterviewTurnStore(InterviewTurnRepository turns, IdGeneratorPort ids, ClockPort clock) {
    this.turns = turns;
    this.ids = ids;
    this.clock = clock;
  }

  /** Steps 4 and 5: a new USER line, or the pending one rewritten; {@code updated_at} touched. */
  @Transactional
  void saveUser(
      UUID sessionId, int turnIndex, boolean reuse, InterviewStage stage, String content) {
    Instant now = clock.now();
    if (reuse) {
      turns.updateUserContent(sessionId, turnIndex, content);
    } else {
      turns.insertTurn(
          new InterviewTurn(
              ids.newId(), sessionId, turnIndex, InterviewTurn.Role.USER, stage, content, now));
    }
    turns.touch(sessionId, now);
  }

  /** Step 11. False, with nothing written, when the session was closed in the meantime. */
  @Transactional
  boolean saveAi(
      UUID sessionId,
      int turnIndex,
      InterviewStage lineStage,
      String content,
      InterviewSessionStage nextStage) {
    Instant now = clock.now();
    if (!turns.advance(sessionId, nextStage, now)) {
      return false;
    }
    turns.insertTurn(
        new InterviewTurn(
            ids.newId(), sessionId, turnIndex, InterviewTurn.Role.AI, lineStage, content, now));
    return true;
  }
}
