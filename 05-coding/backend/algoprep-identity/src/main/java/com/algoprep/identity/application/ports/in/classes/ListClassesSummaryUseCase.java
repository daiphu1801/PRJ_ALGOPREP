package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.domain.model.classes.ClassMetricViews.ClassesSummary;
import java.util.UUID;

/** Totals and one card per class of the instructor (#56). */
public interface ListClassesSummaryUseCase {
  ClassesSummary summary(UUID instructorId);
}
