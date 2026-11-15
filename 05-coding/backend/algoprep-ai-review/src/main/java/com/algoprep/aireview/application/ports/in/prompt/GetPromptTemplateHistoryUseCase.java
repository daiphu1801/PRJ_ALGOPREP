package com.algoprep.aireview.application.ports.in.prompt;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.prompt.PromptTemplate;
import java.util.List;

public interface GetPromptTemplateHistoryUseCase {
  List<PromptTemplate> history(FeatureCode feature);
}
