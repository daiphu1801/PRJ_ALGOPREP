package com.algoprep.aireview.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import org.junit.jupiter.api.Test;

class UnavailableLlmProviderAdapterTest {
  private final UnavailableLlmProviderAdapter adapter = new UnavailableLlmProviderAdapter();

  @Test
  void neverAnswersAndAlwaysReportsAir501() {
    assertThatThrownBy(() -> adapter.completeStructured(null, null, null, null, null))
        .isInstanceOf(AiProviderUnavailableException.class);
    assertThatThrownBy(() -> adapter.streamTurn(null, null, null, null, null).blockFirst())
        .isInstanceOf(AiProviderUnavailableException.class);
  }
}
