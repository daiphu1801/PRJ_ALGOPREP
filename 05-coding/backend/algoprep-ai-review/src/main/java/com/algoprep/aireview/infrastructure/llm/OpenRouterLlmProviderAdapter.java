package com.algoprep.aireview.infrastructure.llm;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException.Reason;
import com.algoprep.aireview.domain.llm.JsonExtractor;
import com.algoprep.aireview.domain.llm.ProviderFailures;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.ChatMemoryRef;
import com.algoprep.aireview.domain.model.LlmUsage;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.model.TokenChunk;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.PerProblemContext;
import com.algoprep.aireview.domain.prompt.SystemInstruction;
import com.algoprep.aireview.domain.prompt.UserData;
import com.openai.errors.OpenAIServiceException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Real {@link LlmProviderPort} over Spring AI's OpenAI-compatible client pointed at OpenRouter
 * (DEC-2026-1108-llm-provider-openrouter-free-models). Registered only when an API key is
 * configured. The system and user messages are exactly the composed strings, one message each; the
 * adapter never rebuilds or concatenates prompt text. Logs carry model id, status, latency and
 * token counts only: never prompt text, never the key.
 */
class OpenRouterLlmProviderAdapter implements LlmProviderPort {
  private static final Logger LOG = LoggerFactory.getLogger(OpenRouterLlmProviderAdapter.class);

  /** Low and fixed: a review should be repeatable, not creative. */
  static final double TEMPERATURE = 0.2;

  private final ChatModel chat;
  private final AiProviderProperties props;

  OpenRouterLlmProviderAdapter(ChatModel chat, AiProviderProperties props) {
    this.chat = chat;
    this.props = props;
  }

  @Override
  public StructuredResult completeStructured(
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      ResponseSchema schema,
      CallOptions options) {
    String model = pickModel(options.modelId());
    Prompt prompt =
        prompt(new ComposedPrompt(system, perProblem, data), model, options.maxTokens(), false);
    long started = System.nanoTime();
    ChatResponse response;
    try {
      response =
          Mono.fromCallable(() -> chat.call(prompt))
              .subscribeOn(Schedulers.boundedElastic())
              .timeout(options.totalTimeout())
              .block();
    } catch (RuntimeException failure) {
      throw failed(failure, model, started);
    }
    String text = textOf(response);
    if (text == null || text.isBlank()) {
      LOG.warn("LLM empty body model={} latencyMs={}", model, millisSince(started));
      throw new AiProviderUnavailableException(Reason.PROVIDER_ERROR, null);
    }
    LlmUsage usage = usageOf(response.getMetadata().getUsage());
    LOG.info(
        "LLM call ok model={} latencyMs={} promptTokens={} completionTokens={}",
        model,
        millisSince(started),
        usage == null ? null : usage.promptTokens(),
        usage == null ? null : usage.completionTokens());
    // Unparseable output is returned as-is: the use case validates it and raises AIR-502, after the
    // tokens were recorded, so the budget never under-counts.
    String json = JsonExtractor.firstJsonObject(text).orElse(text.strip());
    String answeredBy = response.getMetadata().getModel();
    return new StructuredResult(
        json, usage, answeredBy == null || answeredBy.isBlank() ? model : answeredBy);
  }

  /**
   * Interview turn (F5-3 will pass history through {@code memory}); for now the turn is one system
   * plus one user message, exactly as composed.
   */
  @Override
  public Flux<TokenChunk> streamTurn(
      ChatMemoryRef memory,
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      CallOptions options) {
    return Flux.defer(
        () -> {
          String model = pickModel(options.modelId());
          Prompt prompt =
              prompt(
                  new ComposedPrompt(system, perProblem, data), model, options.maxTokens(), true);
          long started = System.nanoTime();
          AtomicReference<LlmUsage> usage = new AtomicReference<>();
          Flux<TokenChunk> bounded = bounded(chat.stream(prompt), options, usage);
          return bounded
              .doOnComplete(
                  () ->
                      LOG.info(
                          "LLM stream ok model={} latencyMs={} usage={}",
                          model,
                          millisSince(started),
                          usage.get()))
              .onErrorMap(
                  e -> !(e instanceof AiProviderUnavailableException),
                  e -> failed(e, model, started));
        });
  }

  /**
   * First-chunk and total deadlines, then one closing chunk carrying usage when it was reported.
   */
  private static Flux<TokenChunk> bounded(
      Flux<ChatResponse> source, CallOptions options, AtomicReference<LlmUsage> usage) {
    AtomicBoolean expired = new AtomicBoolean();
    Flux<TokenChunk> text =
        source
            .doOnNext(
                r -> {
                  LlmUsage u = usageOf(r.getMetadata() == null ? null : r.getMetadata().getUsage());
                  if (u != null) {
                    usage.set(u);
                  }
                })
            .mapNotNull(OpenRouterLlmProviderAdapter::textOf)
            .filter(t -> !t.isEmpty())
            .map(t -> new TokenChunk(t, null))
            .timeout(Mono.delay(options.firstTokenTimeout()), item -> Mono.never());
    return text.takeUntilOther(Mono.delay(options.totalTimeout()).doOnNext(t -> expired.set(true)))
        .concatWith(
            Flux.defer(
                () ->
                    expired.get()
                        ? Flux.error(new TimeoutException("total timeout"))
                        : Flux.empty()))
        .concatWith(
            Flux.defer(
                () ->
                    usage.get() == null
                        ? Flux.empty()
                        : Flux.just(new TokenChunk("", usage.get()))));
  }

  private String pickModel(String requested) {
    if (!props.isAllowed(requested)) {
      LOG.warn("LLM model '{}' not on the allowed list or blank, using default", requested);
    }
    String model = props.resolveModel(requested);
    if (model == null || model.isBlank()) {
      LOG.warn("LLM no usable model: default model is blank");
      throw new AiProviderUnavailableException(Reason.PROVIDER_ERROR, null);
    }
    return model;
  }

  private static Prompt prompt(
      ComposedPrompt composed, String model, int maxTokens, boolean stream) {
    OpenAiChatOptions.Builder options =
        OpenAiChatOptions.builder().model(model).maxTokens(maxTokens).temperature(TEMPERATURE);
    if (stream) {
      options.streamUsage(true);
    }
    return new Prompt(
        List.of(
            new SystemMessage(composed.systemMessage()), new UserMessage(composed.userMessage())),
        options.build());
  }

  private static String textOf(ChatResponse response) {
    Generation result = response == null ? null : response.getResult();
    AssistantMessage output = result == null ? null : result.getOutput();
    return output == null ? null : output.getText();
  }

  private static LlmUsage usageOf(Usage usage) {
    if (usage == null || usage.getPromptTokens() == null || usage.getCompletionTokens() == null) {
      return null;
    }
    int prompt = usage.getPromptTokens();
    int completion = usage.getCompletionTokens();
    // Streams without a usage chunk report 0/0: that is "unknown", not a free call.
    return prompt == 0 && completion == 0 ? null : new LlmUsage(prompt, completion);
  }

  private AiProviderUnavailableException failed(Throwable failure, String model, long started) {
    Integer status = null;
    boolean timedOut = false;
    for (Throwable t = failure; t != null && t.getCause() != t; t = t.getCause()) {
      if (t instanceof OpenAIServiceException service) {
        status = service.statusCode();
      }
      timedOut |=
          t instanceof TimeoutException
              || t instanceof SocketTimeoutException
              || t instanceof HttpTimeoutException
              || t instanceof InterruptedIOException;
    }
    Reason reason = ProviderFailures.classify(status, timedOut);
    // Class name and status only: exception messages may echo the request or response body.
    LOG.warn(
        "LLM call failed model={} status={} reason={} latencyMs={} type={}",
        model,
        status,
        reason,
        millisSince(started),
        failure.getClass().getSimpleName());
    return new AiProviderUnavailableException(reason, null);
  }

  private static long millisSince(long startedNanos) {
    return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
  }
}
