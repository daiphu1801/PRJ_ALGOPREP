package com.algoprep.aireview.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseSchema;
import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.ComposedPrompt;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.PromptComposer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Real Spring wiring: which {@link LlmProviderPort} bean exists, and that the real one works. */
class LlmProviderConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              LlmProviderConfiguration.class, UnavailableLlmProviderAdapter.class);

  @Test
  void blankOrMissingKeyGivesExactlyTheUnavailableFallback() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasNotFailed().hasSingleBean(LlmProviderPort.class);
          assertThat(ctx.getBean(LlmProviderPort.class))
              .isInstanceOf(UnavailableLlmProviderAdapter.class);
        });
    runner
        .withPropertyValues("algoprep.ai.api-key=   ")
        .run(
            ctx ->
                assertThat(ctx.getBean(LlmProviderPort.class))
                    .isInstanceOf(UnavailableLlmProviderAdapter.class));
  }

  @Test
  void aConfiguredKeyGivesExactlyTheOpenRouterAdapterAndBindsTheCommaList() {
    runner
        .withPropertyValues(
            "algoprep.ai.api-key=sk-test",
            "algoprep.ai.base-url=https://openrouter.ai/api",
            "algoprep.ai.default-model=a/b:free",
            "algoprep.ai.models.allowed=a/b:free, c/d:free")
        .run(
            ctx -> {
              assertThat(ctx).hasNotFailed().hasSingleBean(LlmProviderPort.class);
              assertThat(ctx.getBean(LlmProviderPort.class))
                  .isInstanceOf(OpenRouterLlmProviderAdapter.class);
              var props = ctx.getBean(AiProviderProperties.class);
              assertThat(props.models().allowed()).containsExactly("a/b:free", "c/d:free");
              assertThat(props.toString()).doesNotContain("sk-test");
            });
  }

  @Test
  void baseUrlGainsTheV1SuffixOnlyOnce() {
    assertThat(LlmProviderConfiguration.sdkBaseUrl("https://openrouter.ai/api"))
        .isEqualTo("https://openrouter.ai/api/v1");
    assertThat(LlmProviderConfiguration.sdkBaseUrl("https://openrouter.ai/api/v1/"))
        .isEqualTo("https://openrouter.ai/api/v1");
  }

  @Test
  void theWiredAdapterTalksToTheFakeProviderEndToEnd() {
    try (var fake = new FakeOpenAiServer()) {
      fake.body = FakeOpenAiServer.completion("```json\n{\"verdict\":\"OPTIMAL\"}\n```", 11, 22);
      ComposedPrompt prompt =
          new PromptComposer(() -> "n1")
              .compose(
                  new PromptSnapshot(
                      UUID.randomUUID(),
                      FeatureCode.SOLUTION_REVIEW,
                      "sys",
                      null,
                      "x/y:free",
                      "v1"),
                  null,
                  null,
                  List.of(new DataBlock(BlockName.USER_CODE, "code")));
      runner
          .withPropertyValues(
              "algoprep.ai.api-key=sk-test",
              "algoprep.ai.base-url=" + fake.baseUrl(),
              "algoprep.ai.default-model=x/y:free",
              "algoprep.ai.models.allowed=x/y:free")
          .run(
              ctx -> {
                var result =
                    ctx.getBean(LlmProviderPort.class)
                        .completeStructured(
                            prompt.system(),
                            prompt.perProblem(),
                            prompt.data(),
                            new ResponseSchema("solution_review", "v1", "{}"),
                            new CallOptions(
                                "x/y:free", 100, Duration.ofSeconds(5), Duration.ofSeconds(5)));
                assertThat(result.json()).isEqualTo("{\"verdict\":\"OPTIMAL\"}");
                assertThat(result.usage().promptTokens()).isEqualTo(11);
                assertThat(fake.requests.get(0).authorization()).isEqualTo("Bearer sk-test");
              });
    }
  }
}
