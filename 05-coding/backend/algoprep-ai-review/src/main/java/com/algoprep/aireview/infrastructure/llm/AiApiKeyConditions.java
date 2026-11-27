package com.algoprep.aireview.infrastructure.llm;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Exactly one {@code LlmProviderPort} bean per profile: the real adapter when an API key is
 * configured, the "unavailable" fallback otherwise. A plain {@link Condition} (not SpEL) so a key
 * with odd characters can never break expression parsing.
 */
final class AiApiKeyConditions {
  static final String KEY = "algoprep.ai.api-key";

  private AiApiKeyConditions() {}

  static boolean keyPresent(ConditionContext context) {
    return StringUtils.hasText(context.getEnvironment().getProperty(KEY));
  }

  static final class KeyConfigured implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return keyPresent(context);
    }
  }

  static final class KeyMissing implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !keyPresent(context);
    }
  }
}
