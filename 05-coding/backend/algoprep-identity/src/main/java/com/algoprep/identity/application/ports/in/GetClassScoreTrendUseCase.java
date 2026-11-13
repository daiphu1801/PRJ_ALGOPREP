package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassMetricViews.ScoreTrend;
import java.util.UUID;

/** Six completed weeks of average score, for one class or all of them (#57). */
public interface GetClassScoreTrendUseCase {
  ScoreTrend trend(UUID instructorId, UUID classId);
}
