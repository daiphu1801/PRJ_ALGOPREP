package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.PracticeFeedback;
import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.ports.in.CheckAiBudgetUseCase;
import com.algoprep.aireview.application.ports.in.ComparePracticeAnswerUseCase;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.aireview.application.ports.in.RefundPracticeFeedbackUseCase;
import com.algoprep.aireview.application.ports.in.ReservePracticeFeedbackUseCase;
import com.algoprep.aireview.application.ports.out.AiCostGatePort;
import com.algoprep.aireview.domain.exception.AiBudgetExceededException;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.GateOutcome;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * P5 for F6 (DD F6 BR-IB-07 and BR-IB-08). Reuses the F5 Lua gate (hourly quota without cooldown,
 * refund exactly once), the shared budget gate and the token ledger; F6 owns none of them. Not
 * transactional on purpose: the LLM call must never run inside a database transaction.
 */
@Service
class PracticeFeedbackCommandHandler
    implements ReservePracticeFeedbackUseCase,
        RefundPracticeFeedbackUseCase,
        ComparePracticeAnswerUseCase {
  /** The practice feature has no cooldown, only the hourly quota (DD F6 BR-IB-08). */
  private static final int NO_COOLDOWN = 0;

  private static final String SCHEMA_VERSION = "1";

  private final CheckAiBudgetUseCase budget;
  private final AiCostGatePort gate;
  private final LlmProviderPort llm;
  private final RecordAiTokenUsageUseCase usage;
  private final Duration totalTimeout;
  private final PromptComposer composer = new PromptComposer();

  PracticeFeedbackCommandHandler(
      CheckAiBudgetUseCase budget,
      AiCostGatePort gate,
      LlmProviderPort llm,
      RecordAiTokenUsageUseCase usage,
      @Value("${algoprep.ai.llm.timeout.practice-total-sec:90}") long totalTimeoutSec) {
    this.totalTimeout = Duration.ofSeconds(totalTimeoutSec);
    this.budget = budget;
    this.gate = gate;
    this.llm = llm;
    this.usage = usage;
  }

  @Override
  public PracticeFeedback.Ticket reserve(UUID userId, CallerCategory category, int hourlyLimit) {
    CostReservation reservation =
        gate.reserve(userId, FeatureCode.PRACTICE_FEEDBACK, hourlyLimit, NO_COOLDOWN);
    if (reservation.outcome() == GateOutcome.QUOTA_EXCEEDED) {
      return ticket(reservation, PracticeFeedback.Status.QUOTA_EXCEEDED);
    }
    try {
      budget.check(userId, category);
    } catch (AiBudgetExceededException locked) {
      gate.refund(reservation);
      throw locked;
    } catch (RuntimeException failure) {
      gate.refund(reservation);
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.UPSTREAM_PORT, failure);
    }
    return ticket(
        reservation,
        reservation.granted() ? PracticeFeedback.Status.GRANTED : PracticeFeedback.Status.BYPASSED);
  }

  @Override
  public void refund(PracticeFeedback.Ticket ticket) {
    if (!ticket.charged()) {
      return;
    }
    gate.refund(
        new CostReservation(
            GateOutcome.GRANTED,
            ticket.userId(),
            FeatureCode.PRACTICE_FEEDBACK,
            ticket.requestId(),
            ticket.epochHour(),
            true,
            0));
  }

  @Override
  public PracticeFeedback.Output compare(PracticeFeedback.Input input) {
    ComposedPrompt prompt =
        composer.compose(
            new PromptSnapshot(
                input.callId(),
                FeatureCode.PRACTICE_FEEDBACK,
                input.systemInstruction() + "\n\n" + rubricBlock(input.rubric()),
                ResponseGuards.NONE,
                input.modelId(),
                SCHEMA_VERSION),
            null,
            null,
            List.of(
                new DataBlock(
                    BlockName.QUESTION, input.questionTitle() + "\n" + input.questionContent()),
                new DataBlock(BlockName.ANSWER, input.answerText())));
    StructuredResult result = call(prompt, input);
    var tokens = result.usage();
    String model = result.modelName() == null ? input.modelId() : result.modelName();
    usage.record(
        new RecordAiTokenUsageCommand(
            input.callId(),
            input.userId(),
            FeatureCode.PRACTICE_FEEDBACK,
            null,
            input.answerId(),
            tokens == null ? null : tokens.promptTokens(),
            tokens == null ? null : tokens.completionTokens(),
            model));
    return PracticeFeedbackSchema.parse(result.json(), input.rubric(), model);
  }

  private StructuredResult call(ComposedPrompt prompt, PracticeFeedback.Input input) {
    try {
      return llm.completeStructured(
          prompt.system(),
          prompt.perProblem(),
          prompt.data(),
          new ResponseSchema(
              PracticeFeedbackSchema.NAME, SCHEMA_VERSION, PracticeFeedbackSchema.JSON),
          new CallOptions(
              input.modelId(), CallOptions.DEFAULT_MAX_TOKENS, totalTimeout, totalTimeout));
    } catch (AiProviderUnavailableException | AiInvalidResponseException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, failure);
    }
  }

  /** Layer 2 of DD F6 BR-IB-07: text the author typed, so the angle brackets are escaped. */
  static String rubricBlock(List<PracticeFeedback.Criterion> rubric) {
    StringBuilder out = new StringBuilder("<reference_rubric>");
    for (PracticeFeedback.Criterion c : rubric) {
      out.append("\n- criterionCode: ")
          .append(PracticeFeedbackSchema.escape(c.criterionCode()))
          .append("; weightPercent: ")
          .append(c.weightPercent().stripTrailingZeros().toPlainString());
      if (c.description() != null && !c.description().isBlank()) {
        out.append("; description: ").append(PracticeFeedbackSchema.escape(c.description()));
      }
    }
    return out.append("\n</reference_rubric>").toString();
  }

  private static PracticeFeedback.Ticket ticket(CostReservation r, PracticeFeedback.Status status) {
    return new PracticeFeedback.Ticket(
        status, r.userId(), r.requestId(), r.epochHour(), r.retryAfterSeconds());
  }
}
