package com.algoprep.problembank.application.ports.in.student;

import com.algoprep.problembank.application.dto.student.StudentCatalogViews;
import com.algoprep.problembank.domain.model.student.StudentCatalogModels.CatalogSummary;
import java.util.UUID;

/** Progress per topic and per level and the catalogue summary (DD F2 sections 4, 14, 14A). */
public interface StudentCatalogUseCase {
  StudentCatalogViews.TopicProgressList topicProgress(UUID userId);

  StudentCatalogViews.MyLevelProgress levelProgress(UUID userId);

  CatalogSummary summary();
}
