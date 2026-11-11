package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.FeatureCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Endpoint 27: a dense series, oldest day first, every day with all four features. */
public record UsageDaily(int rangeDays, List<Point> points) {
  /** One UTC+7 day. */
  public record Point(LocalDate date, Map<FeatureCode, Long> tokensByFeature) {}
}
