package com.algoprep.aireview.infrastructure.llm;

import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the real provider only when {@code algoprep.ai.api-key} is non-blank. Spring AI's starter
 * (and its auto-configuration) is deliberately not on the classpath: the ChatModel is built by hand
 * here, so a blank key can never fail the context.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProviderProperties.class)
class LlmProviderConfiguration {

  @Bean
  @Conditional(AiApiKeyConditions.KeyConfigured.class)
  ChatModel openRouterChatModel(AiProviderProperties props) {
    OpenAiChatOptions defaults =
        OpenAiChatOptions.builder()
            .apiKey(props.apiKey())
            .baseUrl(sdkBaseUrl(props.baseUrl()))
            .model(props.defaultModel())
            // The use case never retries (BR-AI-02); a hidden second attempt would double the cost.
            .maxRetries(0)
            // Upper bound of the SDK socket timeout; per-call deadlines are enforced by the
            // adapter.
            .timeout(Duration.ofSeconds(120))
            .build();
    return OpenAiChatModel.builder().options(defaults).build();
  }

  @Bean
  @Conditional(AiApiKeyConditions.KeyConfigured.class)
  LlmProviderPort openRouterLlmProvider(ChatModel openRouterChatModel, AiProviderProperties props) {
    return new OpenRouterLlmProviderAdapter(openRouterChatModel, props);
  }

  /**
   * The OpenAI SDK appends {@code /chat/completions}; OpenRouter serves it under {@code /api/v1}.
   */
  static String sdkBaseUrl(String configured) {
    String base = configured == null ? "" : configured.strip().replaceAll("/+$", "");
    return base.endsWith("/v1") ? base : base + "/v1";
  }
}
