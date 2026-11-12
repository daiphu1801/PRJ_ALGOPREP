package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.RubricScore;
import com.algoprep.aireview.domain.model.SessionState;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import com.algoprep.aireview.domain.prompt.ServerDirective;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code scoreAndComplete} of BR-AI-09 steps 4 to 7, shared by endpoint 12 and JOB-AI-04. The
 * caller holds the session lock. One {@code completeStructured} call outside any transaction, the
 * token usage written in its own transaction before the answer is judged, then the single commit.
 * The transcript goes in as data; no budget gate, no quota (DD BR-AI-09 step 3).
 */
@Component
class InterviewScorer {
  /**
   * Default of {@code algoprep.ai.llm.timeout.rubric-total-sec}; a full transcript on a free model
   * is slow.
   */
  static final long DEFAULT_TIMEOUT_SEC = 90;

  private final InterviewSessionRepository sessions;
  private final InterviewScoringRepository scoring;
  private final InterviewScoreStore store;
  private final LlmProviderPort llm;
  private final ScoreCollaborators with;

  /** Field injection keeps the constructor within five parameters; tests set it directly. */
  @Value("${algoprep.ai.llm.timeout.rubric-total-sec:" + DEFAULT_TIMEOUT_SEC + "}")
  long totalTimeoutSec = DEFAULT_TIMEOUT_SEC;

  private final PromptComposer composer = new PromptComposer();

  InterviewScorer(
      InterviewSessionRepository sessions,
      InterviewScoringRepository scoring,
      InterviewScoreStore store,
      LlmProviderPort llm,
      ScoreCollaborators with) {
    this.sessions = sessions;
    this.scoring = scoring;
    this.store = store;
    this.llm = llm;
    this.with = with;
  }

  /**
   * The result when this call closed the session; empty when another writer got there first (the
   * caller re-reads). Throws AIR-501 or AIR-502 with the session untouched.
   *
   * @param observed {@code null} for the learner; the row version the sweep observed
   */
  Optional<InterviewResult> score(InterviewSession session, Instant observed, int idleMinutes) {
    PromptSnapshot snapshot = with.templates().template();
    ComposedPrompt prompt =
        composer.compose(
            snapshot,
            null,
            state(session),
            InterviewMemory.blocks(messages(session)),
            ServerDirective.RUBRIC_JSON);
    UUID callId = with.ids().newId();
    StructuredResult result = call(snapshot, prompt);
    var tokens = result.usage();
    with.usage()
        .record(
            new RecordAiTokenUsageCommand(
                callId,
                session.userId(),
                FeatureCode.MOCK_INTERVIEW,
                null,
                session.id(),
                tokens == null ? null : tokens.promptTokens(),
                tokens == null ? null : tokens.completionTokens(),
                result.modelName() == null ? snapshot.modelId() : result.modelName()));
    List<RubricScore> scores = RubricSchema.parse(result.json());
    return store.commit(session, scores, observed, idleMinutes);
  }

  /** The stored result of a COMPLETED session, or empty while fewer than four rows exist. */
  Optional<InterviewResult> stored(InterviewSession session) {
    List<RubricScore> rows = scoring.rubric(session.id());
    if (rows.size() != 4) {
      return Optional.empty();
    }
    return Optional.of(
        new InterviewResult(session.id(), rows, session.turnCount(), session.endedAt()));
  }

  private StructuredResult call(PromptSnapshot snapshot, ComposedPrompt prompt) {
    Duration total = Duration.ofSeconds(totalTimeoutSec);
    try {
      return llm.completeStructured(
          prompt.system(),
          prompt.perProblem(),
          prompt.data(),
          new ResponseSchema(
              RubricSchema.NAME, snapshot.responseSchemaVersion(), RubricSchema.JSON),
          new CallOptions(snapshot.modelId(), CallOptions.DEFAULT_MAX_TOKENS, total, total));
    } catch (AiProviderUnavailableException | AiInvalidResponseException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, failure);
    }
  }

  /** Context entries from the memory, then every stored line (a pending USER line included). */
  private List<ChatMessage> messages(InterviewSession session) {
    List<ChatMessage> all = new ArrayList<>();
    for (ChatMessage m : with.memory().load(session)) {
      if (m.kind().isContext()) {
        all.add(m);
      }
    }
    for (InterviewTurn line : sessions.turns(session.id())) {
      all.add(
          new ChatMessage(
              line.role() == InterviewTurn.Role.USER ? ChatMessage.Kind.USER : ChatMessage.Kind.AI,
              line.content()));
    }
    return all;
  }

  private SessionState state(InterviewSession s) {
    return new SessionState(
        s.stage().toRunning(),
        sessions.countUserTurns(s.id(), s.stage().toRunning()),
        with.limits().stageTurnCeiling(),
        s.turnCount(),
        s.maxTurns(),
        s.interviewerLevel(),
        s.hintsEnabled(),
        false);
  }
}
