package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.ports.in.GetPromptTemplateHistoryUseCase;
import com.algoprep.aireview.application.ports.in.ListPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoints 14 and 15: stored configuration only, never touches the LLM. */
@Service
@Transactional(readOnly = true)
class PromptTemplateQueryHandlers
    implements ListPromptTemplatesUseCase, GetPromptTemplateHistoryUseCase {
  private final PromptTemplateRepository templates;
  private final AiModelOptionsPort models;

  PromptTemplateQueryHandlers(PromptTemplateRepository templates, AiModelOptionsPort models) {
    this.templates = templates;
    this.models = models;
  }

  @Override
  public PromptTemplateList list() {
    return new PromptTemplateList(
        templates.listLive(), models.allowed(), CallOptions.DEFAULT_MAX_TOKENS);
  }

  @Override
  public List<PromptTemplate> history(FeatureCode feature) {
    return templates.history(feature);
  }
}
