package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.ports.out.LlmProviderPort;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * F5-2: the whole application starts either way, and the key decides which provider bean exists. No
 * network: the key below is a dummy and nothing calls the provider.
 */
class LlmProviderSelectionTest {

  @Nested
  @SpringBootTest(properties = "algoprep.ai.api-key=")
  @ActiveProfiles("test")
  class WithoutKey {
    @Autowired private List<LlmProviderPort> providers;

    @Test
    void theAppStartsWithTheUnavailableFallbackOnly() {
      assertThat(providers).hasSize(1);
      assertThat(providers.get(0).getClass().getSimpleName())
          .isEqualTo("UnavailableLlmProviderAdapter");
    }
  }

  @Nested
  @SpringBootTest(
      properties = {
        "algoprep.ai.api-key=dummy-key-never-sent",
        "algoprep.ai.default-model=vendor/model:free",
        "algoprep.ai.models.allowed=vendor/model:free"
      })
  @ActiveProfiles("test")
  class WithKey {
    @Autowired private List<LlmProviderPort> providers;

    @Test
    void theAppStartsWithTheOpenRouterAdapterOnly() {
      assertThat(providers).hasSize(1);
      assertThat(providers.get(0).getClass().getSimpleName())
          .isEqualTo("OpenRouterLlmProviderAdapter");
    }
  }
}
