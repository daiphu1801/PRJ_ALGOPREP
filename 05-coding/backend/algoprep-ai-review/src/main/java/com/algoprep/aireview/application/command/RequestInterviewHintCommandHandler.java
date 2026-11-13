package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.InterviewHint;
import com.algoprep.aireview.application.ports.in.RequestInterviewHintUseCase;
import com.algoprep.aireview.domain.exception.AiHintLimitReachedException;
import com.algoprep.aireview.domain.exception.AiHintNotAllowedException;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiSessionBusyException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import com.algoprep.aireview.domain.exception.AiTurnLimitReachedException;
import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.SessionState;
import com.algoprep.aireview.domain.ports.out.InterviewSessionLockPort;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.aireview.domain.prompt.ServerDirective;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * BR-AI-08. No budget gate and no hourly quota (a session already paid once at opening). Writes
 * neither {@code interview_turns} nor the ChatMemory; the only state change is {@code hint_count}.
 * Not transactional: the LLM call runs while the session lock is held, outside any database
 * transaction.
 */
@Service
class RequestInterviewHintCommandHandler implements RequestInterviewHintUseCase {
  private final InterviewSessionRepository sessions;
  private final InterviewSessionLockPort locks;
  private final InterviewMemory memory;
  private final InterviewLlmCall llmCall;
  private final InterviewLimits limits;
  private final PromptComposer composer = new PromptComposer();

  RequestInterviewHintCommandHandler(
      InterviewSessionRepository sessions,
      InterviewSessionLockPort locks,
      InterviewMemory memory,
      InterviewLlmCall llmCall,
      InterviewLimits limits) {
    this.sessions = sessions;
    this.locks = locks;
    this.memory = memory;
    this.llmCall = llmCall;
    this.limits = limits;
  }

  @Override
  public InterviewHint hint(UUID callerId, UUID sessionId) {
    InterviewSession first = sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    if (!first.owns(callerId)) {
      throw new AiNotOwnerException();
    }
    check(first);
    return locks
        .withLock(sessionId, () -> hintUnderLock(sessionId))
        .orElseThrow(AiSessionBusyException::new);
  }

  private InterviewHint hintUnderLock(UUID sessionId) {
    // Re-read under the lock: a turn may have finished between the first check and the lock.
    InterviewSession session =
        sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new);
    check(session);
    PromptSnapshot template = llmCall.template();
    List<ChatMessage> history = memory.load(session);
    SessionState state =
        new SessionState(
            session.stage().toRunning(),
            sessions.countUserTurns(sessionId, session.stage().toRunning()),
            limits.stageTurnCeiling(),
            session.turnCount(),
            session.maxTurns(),
            session.interviewerLevel(),
            true,
            false);
    // A SUBMISSION entry never gets here (hint_allowed is NULL), so there is no author brief.
    ComposedPrompt prompt =
        composer.compose(
            template, null, state, InterviewMemory.blocks(history), ServerDirective.HINT_TEXT);
    String raw = llmCall.collect(template, prompt, session.userId(), sessionId);
    String hint = InterviewReplyParser.plainText(raw);
    if (!sessions.incrementHintCount(sessionId, limits.maxHintsPerSession())) {
      // The guarded UPDATE refused: the session ended or the last hint slot was taken.
      check(sessions.find(sessionId).orElseThrow(AiSessionNotFoundException::new));
      throw new AiHintLimitReachedException();
    }
    return new InterviewHint(hint, session.stage());
  }

  /** BR-AI-08 step 1, in the order the DD lists the codes. */
  private void check(InterviewSession session) {
    if (!session.stage().running()) {
      throw new AiSessionNotRunningException();
    }
    if (!session.hintsEnabled()) {
      throw new AiHintNotAllowedException();
    }
    if (session.outOfTurns() || lastStageCeilingReached(session)) {
      throw new AiTurnLimitReachedException();
    }
    if (session.hintCount() >= limits.maxHintsPerSession()) {
      throw new AiHintLimitReachedException();
    }
  }

  private boolean lastStageCeilingReached(InterviewSession session) {
    return session.stage() == InterviewSessionStage.SCALE_UP
        && sessions.countUserTurns(session.id(), session.stage().toRunning())
            >= limits.stageTurnCeiling();
  }
}
