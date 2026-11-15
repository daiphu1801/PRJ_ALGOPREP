package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassMetricViews.ClassesSummary;
import java.util.UUID;

/** Totals and one card per class of the instructor (#56). */
public interface ListClassesSummaryUseCase {
  ClassesSummary summary(UUID instructorId);
}
