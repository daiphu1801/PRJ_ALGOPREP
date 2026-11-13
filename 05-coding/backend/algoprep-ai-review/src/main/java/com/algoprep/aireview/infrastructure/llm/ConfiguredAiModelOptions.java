package com.algoprep.aireview.infrastructure.llm;

import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The alias {@code default} first, then {@code algoprep.ai.models.allowed}; works with a blank API
 * key (the list is just data).
 */
@Component
class ConfiguredAiModelOptions implements AiModelOptionsPort {
  private final AiProviderProperties properties;

  ConfiguredAiModelOptions(AiProviderProperties properties) {
    this.properties = properties;
  }

  @Override
  public List<String> allowed() {
    return Stream.concat(
            Stream.of(AiProviderProperties.DEFAULT_ALIAS), properties.models().allowed().stream())
        .distinct()
        .toList();
  }
}
