package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.ChatMemoryRef;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.model.TokenChunk;
import com.algoprep.aireview.domain.prompt.PerProblemContext;
import com.algoprep.aireview.domain.prompt.SystemInstruction;
import com.algoprep.aireview.domain.prompt.UserData;
import reactor.core.publisher.Flux;

/**
 * Outbound port to the LLM (DD F5 section 3.1). The three prompt layers are three parameters of
 * three different types, so a caller cannot concatenate user text into the system message.
 *
 * <p>Provider failures: timeout or unavailable is reported as {@code AIR-501}, a schema mismatch as
 * {@code AIR-502}; the adapter (slice F5-2, Spring AI over OpenRouter) classifies them. This slice
 * ships no real adapter.
 */
public interface LlmProviderPort {
  StructuredResult completeStructured(
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      ResponseSchema schema,
      CallOptions options);

  /** The last chunk carries usage when the provider reported any. */
  Flux<TokenChunk> streamTurn(
      ChatMemoryRef memory,
      SystemInstruction system,
      PerProblemContext perProblem,
      UserData data,
      CallOptions options);
}
