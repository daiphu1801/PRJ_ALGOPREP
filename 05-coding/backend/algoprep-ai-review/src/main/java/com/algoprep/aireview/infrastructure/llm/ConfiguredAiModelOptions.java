package com.algoprep.aireview.infrastructure.llm;

import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reads {@code algoprep.ai.models.allowed}; works with a blank API key (the list is just data). */
@Component
class ConfiguredAiModelOptions implements AiModelOptionsPort {
  private final AiProviderProperties properties;

  ConfiguredAiModelOptions(AiProviderProperties properties) {
    this.properties = properties;
  }

  @Override
  public List<String> allowed() {
    return properties.models().allowed();
  }
}
