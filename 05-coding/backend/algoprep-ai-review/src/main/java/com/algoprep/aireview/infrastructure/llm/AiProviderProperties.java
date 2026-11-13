package com.algoprep.aireview.infrastructure.llm;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider settings bound from {@code algoprep.ai.*} (application.yml, fed by the ALGOPREP_AI_*
 * environment variables). The API key lives only here, never in the database or the UI
 * (DEC-2026-1010-admin-selects-model-per-prompt); {@link #toString()} never prints it.
 */
@ConfigurationProperties(prefix = "algoprep.ai")
public record AiProviderProperties(
    String apiKey, String baseUrl, String defaultModel, Models models) {
  /** {@code ai.models.allowed} of DD F5 logic Bang 2. */
  public record Models(List<String> allowed) {
    public Models {
      allowed = allowed == null ? List.of() : List.copyOf(allowed);
    }
  }

  public AiProviderProperties {
    models = models == null ? new Models(List.of()) : models;
  }

  /**
   * The seed alias (V5.002) always valid and always listed; it resolves to {@link #defaultModel}
   * (DEC-2026-1112-ai-model-alias-default-always-allowed).
   */
  public static final String DEFAULT_ALIAS = "default";

  /** The requested model when allowed, otherwise the default (may be blank). */
  String resolveModel(String requested) {
    if (isAllowed(requested) && !DEFAULT_ALIAS.equals(requested)) {
      return requested;
    }
    return defaultModel;
  }

  boolean isAllowed(String requested) {
    return requested != null
        && !requested.isBlank()
        && (DEFAULT_ALIAS.equals(requested) || models.allowed().contains(requested));
  }

  @Override
  public String toString() {
    return "AiProviderProperties[baseUrl=" + baseUrl + ", defaultModel=" + defaultModel + "]";
  }
}
