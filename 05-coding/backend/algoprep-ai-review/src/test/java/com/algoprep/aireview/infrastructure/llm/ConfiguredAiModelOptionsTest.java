package com.algoprep.aireview.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The seed alias 'default' is always listed first and never duplicated. */
class ConfiguredAiModelOptionsTest {
  private static ConfiguredAiModelOptions options(String... allowed) {
    return new ConfiguredAiModelOptions(
        new AiProviderProperties(
            "k", "http://x", "d/m", new AiProviderProperties.Models(List.of(allowed))));
  }

  @Test
  void aliasComesFirstEvenWhenTheConfiguredListIsEmpty() {
    assertThat(options().allowed()).containsExactly("default");
  }

  @Test
  void aliasIsPrependedToTheConfiguredIds() {
    assertThat(options("a/b:free", "c/d:free").allowed())
        .containsExactly("default", "a/b:free", "c/d:free");
  }

  @Test
  void aliasConfiguredExplicitlyIsNotDuplicated() {
    assertThat(options("a/b:free", "default").allowed()).containsExactly("default", "a/b:free");
  }
}
