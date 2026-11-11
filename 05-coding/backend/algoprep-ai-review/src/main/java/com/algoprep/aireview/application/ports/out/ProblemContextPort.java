package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.ProblemContext;
import java.util.UUID;

/** Outbound port served by problem-bank (DD F5 section 1.3, DD F2 section 58.1). */
public interface ProblemContextPort {
  /**
   * Any failure, including a problem that is no longer published, surfaces as {@code
   * AiProviderUnavailableException(UPSTREAM_PORT)} (AIR-501): without context nothing is generated.
   */
  ProblemContext getContext(UUID problemId, String language);
}
