package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptTemplateQueryHandlersTest {
  private final PromptTemplateRepository repo = mock(PromptTemplateRepository.class);
  private final AiModelOptionsPort models = mock(AiModelOptionsPort.class);
  private final PromptTemplateQueryHandlers handlers =
      new PromptTemplateQueryHandlers(repo, models);

  @Test
  void listCarriesTheAllowedModelsAndTheReadOnlyTokenCap() {
    when(models.allowed()).thenReturn(List.of("a", "b"));
    when(repo.listLive()).thenReturn(List.of());
    var result = handlers.list();
    assertThat(result.modelOptions()).containsExactly("a", "b");
    assertThat(result.maxTokens()).isEqualTo(4096);
  }

  @Test
  void historyIsReadStraightFromTheRepository() {
    when(repo.history(FeatureCode.MOCK_INTERVIEW)).thenReturn(List.of());
    assertThat(handlers.history(FeatureCode.MOCK_INTERVIEW)).isEmpty();
  }
}
