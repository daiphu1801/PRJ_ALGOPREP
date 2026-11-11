package com.algoprep.aireview.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.model.StructuredResult;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/**
 * Opt-in smoke test: ONE real request to the configured provider. It runs only when the API key is
 * present in the process environment, so a normal build never touches the network. It prints the
 * model id, latency, token counts and whether the answer parsed as JSON, never the key and never
 * the model's text.
 *
 * <p>Run (values come from 05-coding/.env, never from this file): export the four {@code
 * ALGOPREP_AI_*} variables, then {@code ./mvnw -pl algoprep-ai-review test
 * -Dtest=OpenRouterLiveSmokeTest}.
 */
@EnabledIfEnvironmentVariable(named = "ALGOPREP_AI_OPENROUTER_API_KEY", matches = ".+")
class OpenRouterLiveSmokeTest {

  @Test
  void oneRealStructuredCall() {
    String model = System.getenv("ALGOPREP_AI_DEFAULT_MODEL");
    assertThat(model).as("ALGOPREP_AI_DEFAULT_MODEL must be set").isNotBlank();
    OpenRouterLlmProviderAdapter adapter = realAdapter(model);
    ComposedPrompt prompt = smokePrompt(model);

    long start = System.nanoTime();
    StructuredResult result =
        adapter.completeStructured(
            prompt.system(),
            prompt.perProblem(),
            prompt.data(),
            new ResponseSchema("smoke", "v1", "{}"),
            new CallOptions(model, 200, Duration.ofSeconds(60), Duration.ofSeconds(60)));
    report(result, (System.nanoTime() - start) / 1_000_000);
    assertThat(result.json()).isNotBlank();
  }

  private static OpenRouterLlmProviderAdapter realAdapter(String model) {
    List<String> allowed =
        Arrays.stream(envOr("ALGOPREP_AI_ALLOWED_MODELS", model).split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .toList();
    var props =
        new AiProviderProperties(
            System.getenv("ALGOPREP_AI_OPENROUTER_API_KEY"),
            envOr("ALGOPREP_AI_BASE_URL", "https://openrouter.ai/api"),
            model,
            new AiProviderProperties.Models(allowed));
    return new OpenRouterLlmProviderAdapter(
        new LlmProviderConfiguration().openRouterChatModel(props), props);
  }

  private static ComposedPrompt smokePrompt(String model) {
    String instruction =
        "Answer with one JSON object only, no prose, no code fence. "
            + "Shape: {\"ok\": true, \"language\": \"<language of the code>\"}.";
    return new PromptComposer()
        .compose(
            new PromptSnapshot(
                UUID.randomUUID(), FeatureCode.SOLUTION_REVIEW, instruction, null, model, "smoke"),
            null,
            null,
            List.of(new DataBlock(BlockName.USER_CODE, "print(1)")));
  }

  private static void report(StructuredResult result, long latencyMs) {
    String usage =
        result.usage() == null
            ? "none"
            : result.usage().promptTokens() + "+" + result.usage().completionTokens();
    System.out.println(
        "LIVE-SMOKE model="
            + result.modelName()
            + " latencyMs="
            + latencyMs
            + " usageTokens="
            + usage
            + " textChars="
            + (result.json() == null ? 0 : result.json().length())
            + " jsonParses="
            + parses(result.json()));
  }

  private static boolean parses(String json) {
    try {
      return JsonMapper.builder().build().readTree(json).isObject();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static String envOr(String name, String fallback) {
    String v = System.getenv(name);
    return v == null || v.isBlank() ? fallback : v;
  }
}
