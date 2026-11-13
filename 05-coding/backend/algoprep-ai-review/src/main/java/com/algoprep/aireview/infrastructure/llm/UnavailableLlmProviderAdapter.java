package com.algoprep.aireview.infrastructure.llm;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.ChatMemoryRef;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.model.TokenChunk;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.PerProblemContext;
import com.algoprep.aireview.domain.prompt.SystemInstruction;
import com.algoprep.aireview.domain.prompt.UserData;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Fallback used when no API key is configured (slice F5-2 registers the OpenRouter adapter
 * otherwise). It generates nothing: every call reports the provider as unavailable (AIR-501,
 * PROVIDER_ERROR), so the generating endpoints answer 503 while F1-F4 and every stored-data read of
 * F5 keep working. It is not a fake answer generator and must never return content.
 */
@Component
@Conditional(AiApiKeyConditions.KeyMissing.class)
class UnavailableLlmProviderAdapter implements LlmProviderPort {
  @Override
  public StructuredResult completeStructured(
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      ResponseSchema schema,
      CallOptions options) {
    throw unavailable();
  }

  @Override
  public Flux<TokenChunk> streamTurn(
      ChatMemoryRef memory,
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      CallOptions options) {
    return Flux.error(unavailable());
  }

  private static AiProviderUnavailableException unavailable() {
    return new AiProviderUnavailableException(
        AiProviderUnavailableException.Reason.PROVIDER_ERROR, null);
  }
}
