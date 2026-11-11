package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptTemplate;
import java.util.List;

public interface GetPromptTemplateHistoryUseCase {
  List<PromptTemplate> history(FeatureCode feature);
}
