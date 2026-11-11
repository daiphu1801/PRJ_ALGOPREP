package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.InterviewSessionDetail;
import com.algoprep.aireview.application.ports.in.GetInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.out.InterviewReplyTracker;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.ReplyState;
import com.algoprep.aireview.domain.model.RubricScore;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 9: Postgres only (never Redis, never the LLM), so it works while the provider is down.
 */
@Service
@Transactional(readOnly = true)
class GetInterviewSessionQueryHandler implements GetInterviewSessionUseCase {
  private final InterviewSessionRepository sessions;
  private final InterviewReplyTracker tracker;
  private final InterviewScoringRepository scoring;

  GetInterviewSessionQueryHandler(
      InterviewSessionRepository sessions,
      InterviewReplyTracker tracker,
      InterviewScoringRepository scoring) {
    this.sessions = sessions;
    this.tracker = tracker;
    this.scoring = scoring;
  }

  @Override
  public InterviewSessionDetail get(UUID callerId, UUID sessionId) {
    InterviewSession session =
        sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    if (!session.owns(callerId)) {
      throw new AiNotOwnerException();
    }
    List<InterviewTurn> turns = sessions.turns(sessionId);
    return new InterviewSessionDetail(session, turns, replyState(session, turns), result(session));
  }

  /** BR-AI-09 step 9: the four stored rows, or null while a COMPLETED session has fewer. */
  private InterviewResult result(InterviewSession session) {
    if (session.stage() != InterviewSessionStage.COMPLETED) {
      return null;
    }
    List<RubricScore> rows = scoring.rubric(session.id());
    return rows.size() == 4
        ? new InterviewResult(session.id(), rows, session.turnCount(), session.endedAt())
        : null;
  }

  /** GENERATING while a turn runs; FAILED when the last line is a USER line nobody answered. */
  private ReplyState replyState(InterviewSession session, List<InterviewTurn> turns) {
    if (tracker.isGenerating(session.id())) {
      return ReplyState.GENERATING;
    }
    boolean unanswered =
        session.stage().running()
            && !turns.isEmpty()
            && turns.get(turns.size() - 1).role() == InterviewTurn.Role.USER;
    return unanswered ? ReplyState.FAILED : ReplyState.IDLE;
  }
}
