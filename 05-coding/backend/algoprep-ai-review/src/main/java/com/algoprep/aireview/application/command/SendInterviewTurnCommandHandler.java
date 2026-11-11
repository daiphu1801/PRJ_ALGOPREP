package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.SendInterviewTurnCommand;
import com.algoprep.aireview.application.dto.TurnAccepted;
import com.algoprep.aireview.application.dto.TurnCompleted;
import com.algoprep.aireview.application.ports.in.SendInterviewTurnUseCase;
import com.algoprep.aireview.application.ports.out.InterviewReplyTracker;
import com.algoprep.aireview.application.ports.out.InterviewTurnSink;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiSessionBusyException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import com.algoprep.aireview.domain.exception.AiTurnCountMismatchException;
import com.algoprep.aireview.domain.exception.AiTurnLimitReachedException;
import com.algoprep.aireview.domain.llm.TurnMessageCutter;
import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.SessionState;
import com.algoprep.aireview.domain.model.TurnProgression;
import com.algoprep.aireview.domain.ports.out.InterviewSessionLockPort;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.ProblemGuidance;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.aireview.domain.prompt.ServerDirective;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * BR-AI-07: one Mock Interview turn. Not transactional: the USER line and the AI line are two short
 * transactions in {@link InterviewTurnStore}; the LLM stream runs between them while the session
 * lock is held. The learner's answer is a data block, never part of the system message. The soft
 * budget lock is checked on every turn, before anything is written
 * (DEC-2026-1110-f5-turn-budget-gate-and-cancel-on-disconnect); cooldown and hourly quota are not,
 * so there is nothing to refund. A failed or abandoned turn (error, or the client disconnected and
 * the LLM stream was cancelled) leaves the stage, {@code turn_count} and the USER line as they were
 * and writes no AI line.
 */
@Service
class SendInterviewTurnCommandHandler implements SendInterviewTurnUseCase {
  private static final Logger LOG =
      Logger.getLogger(SendInterviewTurnCommandHandler.class.getName());

  private final InterviewSessionRepository sessions;
  private final InterviewSessionLockPort locks;
  private final InterviewTurnStore store;
  private final TurnCollaborators with;
  private final InterviewReplyTracker tracker;
  private final PromptComposer composer = new PromptComposer();

  SendInterviewTurnCommandHandler(
      InterviewSessionRepository sessions,
      InterviewSessionLockPort locks,
      InterviewTurnStore store,
      TurnCollaborators with,
      InterviewReplyTracker tracker) {
    this.sessions = sessions;
    this.locks = locks;
    this.store = store;
    this.with = with;
    this.tracker = tracker;
  }

  @Override
  public void send(SendInterviewTurnCommand command, InterviewTurnSink sink) {
    InterviewSession first =
        sessions.find(command.sessionId()).orElseThrow(AiSessionNotFoundException::new);
    if (!first.owns(command.callerId())) {
      throw new AiNotOwnerException();
    }
    if (!first.stage().running()) {
      throw new AiSessionNotRunningException();
    }
    locks
        .withLock(
            command.sessionId(),
            () -> {
              underLock(command, sink);
              return Boolean.TRUE;
            })
        .orElseThrow(AiSessionBusyException::new);
  }

  private void underLock(SendInterviewTurnCommand command, InterviewTurnSink sink) {
    // Re-read under the lock: another turn may have finished while this call waited for it.
    InterviewSession session =
        sessions.find(command.sessionId()).orElseThrow(AiSessionNotFoundException::new);
    if (!session.stage().running()) {
      throw new AiSessionNotRunningException();
    }
    if (command.expectedTurnCount() != session.turnCount()) {
      throw new AiTurnCountMismatchException(session.turnCount());
    }
    InterviewStage stage = session.stage().toRunning();
    if (session.outOfTurns()
        || (stage == InterviewStage.SCALE_UP
            && sessions.countUserTurns(session.id(), stage) >= with.limits().stageTurnCeiling())) {
      throw new AiTurnLimitReachedException();
    }
    with.budget().check(session.userId(), command.category());
    // Everything that can fail for upstream reasons is resolved before a line is written, so
    // those failures are plain JSON errors and leave the session untouched.
    PromptSnapshot template = with.llm().template();
    List<ChatMessage> history = withoutPendingUser(with.memory().load(session));
    ProblemGuidance guidance = with.memory().guidanceFor(session);
    List<InterviewTurn> lines = sessions.turns(session.id());
    InterviewTurn last = lines.get(lines.size() - 1);
    boolean reuse = last.role() == InterviewTurn.Role.USER;
    int userIndex = reuse ? last.turnIndex() : last.turnIndex() + 1;
    boolean forced = lastAiStage(lines) != stage;

    tracker.begin(session.id());
    try {
      store.saveUser(session.id(), userIndex, reuse, stage, command.content());
      sink.accepted(
          new TurnAccepted(
              session.id(),
              userIndex,
              userIndex + 1,
              stage.name(),
              session.turnCount(),
              session.maxTurns()));
      try {
        stream(
            new Turn(command, session, stage, template, history, guidance, forced, userIndex),
            sink);
      } catch (RuntimeException failure) {
        LOG.log(Level.FINE, "Interview turn failed after accept", failure);
        sink.failed(failure);
      }
    } finally {
      tracker.end(session.id());
    }
  }

  /** Everything a running turn needs, resolved before the USER line was written. */
  private record Turn(
      SendInterviewTurnCommand command,
      InterviewSession session,
      InterviewStage stage,
      PromptSnapshot template,
      List<ChatMessage> history,
      ProblemGuidance guidance,
      boolean forced,
      int userIndex) {}

  private void stream(Turn turn, InterviewTurnSink sink) {
    InterviewSession session = turn.session();
    int stageTurns = sessions.countUserTurns(session.id(), turn.stage());
    TurnMessageCutter cutter = new TurnMessageCutter();
    String raw =
        with.llm()
            .collectLive(
                turn.template(),
                prompt(turn, stageTurns),
                session.userId(),
                session.id(),
                new InterviewLlmCall.Live(
                    chunk -> {
                      String piece = cutter.feed(chunk);
                      if (!piece.isEmpty()) {
                        sink.token(piece);
                      }
                    },
                    sink.disconnected()));
    if (sink.disconnected().isDone()) {
      // Client gone: the Flux was cancelled, no AI line, the USER line stays (replyState FAILED).
      return;
    }
    InterviewReplyParser.Reply reply = InterviewReplyParser.parse(raw);
    if (!cutter.streamedAnything()) {
      // Message was not the first key (or not recognised): one token with the parsed text.
      sink.token(reply.message());
    }
    commit(turn, stageTurns, reply, sink);
  }

  private ComposedPrompt prompt(Turn turn, int stageTurns) {
    InterviewSession session = turn.session();
    SessionState state =
        new SessionState(
            turn.stage(),
            stageTurns,
            with.limits().stageTurnCeiling(),
            session.turnCount(),
            session.maxTurns(),
            session.interviewerLevel(),
            session.hintsEnabled(),
            turn.forced());
    List<DataBlock> blocks = new ArrayList<>(InterviewMemory.blocks(turn.history()));
    blocks.add(new DataBlock(BlockName.ANSWER, turn.command().content()));
    return composer.compose(
        turn.template(), turn.guidance(), state, blocks, ServerDirective.TURN_JSON);
  }

  /** BR-AI-07 steps 10 to 13: decide the stage, commit the AI line, remember, report. */
  private void commit(
      Turn turn, int stageTurns, InterviewReplyParser.Reply reply, InterviewTurnSink sink) {
    InterviewSession session = turn.session();
    int turnCountAfter = session.turnCount() + 1;
    TurnProgression next =
        TurnProgression.decide(
            new TurnProgression.Facts(
                turn.stage(),
                reply.stageComplete(),
                stageTurns,
                with.limits().stageTurnCeiling(),
                turnCountAfter,
                session.maxTurns()));
    int aiIndex = turn.userIndex() + 1;
    if (!store.saveAi(
        session.id(), aiIndex, next.aiLineStage(), reply.message(), next.nextStage())) {
      throw new AiSessionNotRunningException();
    }
    writeMemory(session.id(), turn.history(), turn.command().content(), reply.message());
    sink.completed(
        new TurnCompleted(
            aiIndex,
            reply.message(),
            next.nextStage().name(),
            next.stageChanged(),
            turnCountAfter,
            session.maxTurns(),
            next.shouldComplete()));
  }

  private void writeMemory(UUID sessionId, List<ChatMessage> history, String answer, String ai) {
    List<ChatMessage> updated = new ArrayList<>(history);
    updated.add(new ChatMessage(ChatMessage.Kind.USER, answer));
    updated.add(new ChatMessage(ChatMessage.Kind.AI, ai));
    try {
      with.memory().write(sessionId, updated);
    } catch (RuntimeException redisDown) {
      LOG.log(Level.WARNING, "ChatMemory write failed; it is rebuilt from PostgreSQL", redisDown);
    }
  }

  /** A pending USER line (earlier turn failed) is re-sent as the answer block, not twice. */
  private static List<ChatMessage> withoutPendingUser(List<ChatMessage> loaded) {
    if (!loaded.isEmpty() && loaded.get(loaded.size() - 1).kind() == ChatMessage.Kind.USER) {
      return loaded.subList(0, loaded.size() - 1);
    }
    return loaded;
  }

  /**
   * The stage of the last AI line. It differs from the session stage only after a forced change,
   * where the AI line keeps the old stage (BR-AI-07 step 10).
   */
  private static InterviewStage lastAiStage(List<InterviewTurn> lines) {
    for (int i = lines.size() - 1; i >= 0; i--) {
      if (lines.get(i).role() == InterviewTurn.Role.AI) {
        return lines.get(i).stage();
      }
    }
    return null;
  }
}
