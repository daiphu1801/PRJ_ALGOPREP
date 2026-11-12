package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.ChatMemoryRef;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LlmUsage;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.TokenChunk;
import com.algoprep.aireview.domain.ports.out.AiPromptSnapshotRepository;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * One short interview call (opening line or hint): {@code streamTurn} gathered into a string, then
 * the token usage written in its own transaction BEFORE the caller judges the text, so a bad answer
 * still counts against the budget (BR-AI-01). No retry.
 */
@Component
class InterviewLlmCall {
  /** Defaults of {@code interview-call-total-sec} and {@code interview-first-token-sec}. */
  static final long DEFAULT_CALL_TOTAL_SEC = 60;

  static final long DEFAULT_FIRST_TOKEN_SEC = 30;

  static final Duration TURN_TOTAL_TIMEOUT = Duration.ofSeconds(60);

  private final AiPromptSnapshotRepository templates;
  private final LlmProviderPort llm;
  private final RecordAiTokenUsageUseCase usage;
  private final IdGeneratorPort ids;

  /** Field injection keeps the constructor within five parameters; tests set them directly. */
  @Value("${algoprep.ai.llm.timeout.interview-call-total-sec:" + DEFAULT_CALL_TOTAL_SEC + "}")
  long callTotalSec = DEFAULT_CALL_TOTAL_SEC;

  @Value("${algoprep.ai.llm.timeout.interview-first-token-sec:" + DEFAULT_FIRST_TOKEN_SEC + "}")
  long firstTokenSec = DEFAULT_FIRST_TOKEN_SEC;

  InterviewLlmCall(
      AiPromptSnapshotRepository templates,
      LlmProviderPort llm,
      RecordAiTokenUsageUseCase usage,
      IdGeneratorPort ids) {
    this.templates = templates;
    this.llm = llm;
    this.usage = usage;
    this.ids = ids;
  }

  /** Where a live turn sends its text, and the signal that cancels it (client disconnect). */
  record Live(Consumer<String> onText, CompletableFuture<Void> gone) {}

  /** The ACTIVE MOCK_INTERVIEW template, read once per call; none is AIR-501 (BR-AI-01 step 1). */
  PromptSnapshot template() {
    return templates
        .readActive(FeatureCode.MOCK_INTERVIEW)
        .orElseThrow(
            () ->
                new AiProviderUnavailableException(
                    AiProviderUnavailableException.Reason.UPSTREAM_PORT, null));
  }

  /** Raw model text. Throws AIR-501 for any provider failure, including a blown deadline. */
  String collect(PromptSnapshot template, ComposedPrompt prompt, UUID userId, UUID sessionId) {
    UUID callId = ids.newId();
    List<TokenChunk> chunks = stream(template, prompt, sessionId);
    StringBuilder text = new StringBuilder();
    LlmUsage tokens = null;
    for (TokenChunk chunk : chunks) {
      if (chunk.text() != null) {
        text.append(chunk.text());
      }
      if (chunk.usage() != null) {
        tokens = chunk.usage();
      }
    }
    usage.record(
        new RecordAiTokenUsageCommand(
            callId,
            userId,
            FeatureCode.MOCK_INTERVIEW,
            null,
            sessionId,
            tokens == null ? null : tokens.promptTokens(),
            tokens == null ? null : tokens.completionTokens(),
            template.modelId()));
    return text.toString();
  }

  /**
   * One live turn (BR-AI-07 steps 7 and 9): each text chunk goes to {@code onText} as it arrives;
   * returns the whole raw reply. The usage row is written in its own transaction whenever the
   * provider reported usage, even if the reply is judged bad afterwards, and never otherwise. Any
   * provider failure, including a blown deadline, is AIR-501. When {@code gone} completes (client
   * disconnected) the Flux is cancelled and the text gathered so far is returned; the caller must
   * check {@code gone} and discard it. Usage reported before the cancel is still recorded.
   */
  String collectLive(
      PromptSnapshot template, ComposedPrompt prompt, UUID userId, UUID sessionId, Live live) {
    UUID callId = ids.newId();
    StringBuilder text = new StringBuilder();
    LlmUsage[] tokens = new LlmUsage[1];
    try {
      llm.streamTurn(
              new ChatMemoryRef(sessionId),
              prompt.system(),
              prompt.perProblem(),
              prompt.data(),
              new CallOptions(
                  template.modelId(),
                  CallOptions.DEFAULT_MAX_TOKENS,
                  Duration.ofSeconds(firstTokenSec),
                  TURN_TOTAL_TIMEOUT))
          .takeUntilOther(Mono.fromFuture(live.gone().thenApply(none -> Boolean.TRUE)))
          .doOnNext(
              chunk -> {
                if (chunk.usage() != null) {
                  tokens[0] = chunk.usage();
                }
                if (chunk.text() != null && !chunk.text().isEmpty()) {
                  text.append(chunk.text());
                  live.onText().accept(chunk.text());
                }
              })
          .blockLast(TURN_TOTAL_TIMEOUT.plusSeconds(5));
    } catch (AiProviderUnavailableException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw classify(failure);
    } finally {
      usage.record(
          new RecordAiTokenUsageCommand(
              callId,
              userId,
              FeatureCode.MOCK_INTERVIEW,
              null,
              sessionId,
              tokens[0] == null ? null : tokens[0].promptTokens(),
              tokens[0] == null ? null : tokens[0].completionTokens(),
              template.modelId()));
    }
    return text.toString();
  }

  private static AiProviderUnavailableException classify(RuntimeException failure) {
    // Reactor reports an expired block(Duration) as an IllegalStateException with this prefix.
    boolean expired =
        failure instanceof IllegalStateException
            && failure.getMessage() != null
            && failure.getMessage().startsWith("Timeout on blocking");
    return new AiProviderUnavailableException(
        expired
            ? AiProviderUnavailableException.Reason.TIMEOUT
            : AiProviderUnavailableException.Reason.PROVIDER_ERROR,
        failure);
  }

  private List<TokenChunk> stream(PromptSnapshot template, ComposedPrompt prompt, UUID sessionId) {
    List<TokenChunk> chunks;
    try {
      chunks =
          llm.streamTurn(
                  new ChatMemoryRef(sessionId),
                  prompt.system(),
                  prompt.perProblem(),
                  prompt.data(),
                  new CallOptions(
                      template.modelId(),
                      CallOptions.DEFAULT_MAX_TOKENS,
                      Duration.ofSeconds(firstTokenSec),
                      Duration.ofSeconds(callTotalSec)))
              .collectList()
              .block(Duration.ofSeconds(callTotalSec + 5));
    } catch (AiProviderUnavailableException failure) {
      throw failure;
    } catch (IllegalStateException failure) {
      // Reactor reports an expired block(Duration) as an IllegalStateException with this prefix.
      boolean expired =
          failure.getMessage() != null && failure.getMessage().startsWith("Timeout on blocking");
      throw new AiProviderUnavailableException(
          expired
              ? AiProviderUnavailableException.Reason.TIMEOUT
              : AiProviderUnavailableException.Reason.PROVIDER_ERROR,
          failure);
    } catch (RuntimeException failure) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, failure);
    }
    if (chunks == null) {
      throw new AiProviderUnavailableException(
          AiProviderUnavailableException.Reason.PROVIDER_ERROR, null);
    }
    return chunks;
  }
}
