package com.algoprep.aireview.application.command.interview;

import com.algoprep.aireview.application.command.InterviewCostGate;
import com.algoprep.aireview.application.dto.interview.OpenInterviewSessionCommand;
import com.algoprep.aireview.application.dto.interview.OpenedInterviewSession;
import com.algoprep.aireview.application.ports.in.interview.OpenInterviewSessionUseCase;
import com.algoprep.aireview.domain.model.budget.CostReservation;
import com.algoprep.aireview.domain.model.interview.ChatMessage;
import com.algoprep.aireview.domain.model.interview.InterviewSession;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import com.algoprep.aireview.domain.model.interview.InterviewStage;
import com.algoprep.aireview.domain.model.interview.InterviewTurn;
import com.algoprep.aireview.domain.model.interview.SessionState;
import com.algoprep.aireview.domain.model.prompt.PromptSnapshot;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.ProblemGuidance;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.aireview.domain.prompt.ServerDirective;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * BR-AI-06. Deliberately not transactional: the LLM call stays outside any transaction and the
 * session plus its opening line are written by {@link InterviewSessionStore} only after a valid
 * opening arrived. A QUESTION_BANK entry never reads interview-bank: the question text comes from
 * the request and is stored as data (BR-AI-06 step 2).
 */
@Service
class OpenInterviewSessionCommandHandler implements OpenInterviewSessionUseCase {
  private final InterviewMemory memory;
  private final InterviewCostGate costGate;
  private final InterviewLlmCall llmCall;
  private final InterviewSessionStore store;
  private final InterviewLimits limits;
  private final PromptComposer composer = new PromptComposer();

  OpenInterviewSessionCommandHandler(
      InterviewMemory memory,
      InterviewCostGate costGate,
      InterviewLlmCall llmCall,
      InterviewSessionStore store,
      InterviewLimits limits) {
    this.memory = memory;
    this.costGate = costGate;
    this.llmCall = llmCall;
    this.store = store;
    this.limits = limits;
  }

  @Override
  public OpenedInterviewSession open(OpenInterviewSessionCommand command) {
    InterviewMemory.Opening opening = memory.openingFor(command);
    PromptSnapshot template = llmCall.template();
    CostReservation reservation = costGate.reserve(command.callerId(), command.category());
    try {
      return generate(command, template, opening.guidance(), opening.context());
    } catch (RuntimeException failure) {
      costGate.refund(reservation);
      throw failure;
    }
  }

  private OpenedInterviewSession generate(
      OpenInterviewSessionCommand c,
      PromptSnapshot template,
      ProblemGuidance guidance,
      List<ChatMessage> context) {
    UUID sessionId = store.newId();
    SessionState state =
        new SessionState(
            InterviewStage.EXPLAIN,
            0,
            limits.stageTurnCeiling(),
            0,
            c.maxTurns(),
            c.interviewerLevel(),
            Boolean.TRUE.equals(c.hintAllowed()),
            false);
    ComposedPrompt prompt =
        composer.compose(
            template, guidance, state, InterviewMemory.blocks(context), ServerDirective.TURN_JSON);
    String raw = llmCall.collect(template, prompt, c.callerId(), sessionId);
    // stageComplete of the opening line is ignored (BR-AI-06 step 4).
    String message = InterviewReplyParser.parse(raw).message();
    Instant now = store.now();
    InterviewSession session = newSession(c, sessionId, now);
    InterviewTurn opening =
        new InterviewTurn(
            store.newId(),
            sessionId,
            1,
            InterviewTurn.Role.AI,
            InterviewStage.EXPLAIN,
            message,
            now);
    store.saveOpened(session, opening);
    List<ChatMessage> remembered = new ArrayList<>(context);
    remembered.add(new ChatMessage(ChatMessage.Kind.AI, message));
    memory.write(sessionId, remembered);
    return new OpenedInterviewSession(session, opening);
  }

  private static InterviewSession newSession(
      OpenInterviewSessionCommand c, UUID sessionId, Instant now) {
    return new InterviewSession(
        sessionId,
        c.callerId(),
        c.entryType(),
        c.submissionId(),
        c.questionRef(),
        c.questionText(),
        c.topics(),
        c.interviewerLevel(),
        c.maxTurns(),
        c.hintAllowed(),
        0,
        InterviewSessionStage.EXPLAIN,
        0,
        now,
        null,
        now);
  }
}
