package com.algoprep.judge.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.MySubmissionMetricQuery;
import com.algoprep.judge.application.dto.MySubmissionMetrics.TopicMetric;
import com.algoprep.judge.application.ports.in.GetMySubmissionMetricsUseCase;
import com.algoprep.judge.domain.model.SubmissionActivityDay;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('STUDENT')")
class MySubmissionMetricsController {
  private final GetMySubmissionMetricsUseCase metrics;

  MySubmissionMetricsController(GetMySubmissionMetricsUseCase metrics) {
    this.metrics = metrics;
  }

  @GetMapping("/api/v1/judge/me/metrics")
  Response read(
      @RequestParam(required = false) String range,
      @RequestParam(required = false) List<String> topicIds) {
    var result = metrics.read(new MySubmissionMetricQuery(range(range), topics(topicIds)));
    return new Response(
        result.streakDays(),
        result.lastSubmittedAt(),
        result.dailyCounts(),
        result.acRateByTopic());
  }

  private static SubmissionMetricRange range(String value) {
    return switch (value == null ? "" : value) {
      case "7d" -> SubmissionMetricRange.SEVEN_DAYS;
      case "30d" -> SubmissionMetricRange.THIRTY_DAYS;
      case "all" -> SubmissionMetricRange.ALL;
      default -> throw new InvalidRequestException("JDG-V07", "Invalid metrics range");
    };
  }

  private static Set<UUID> topics(List<String> values) {
    if (values == null) {
      return Set.of();
    }
    if (values.size() > 50) {
      throw new InvalidRequestException("JDG-V15", "Invalid metrics topics");
    }
    var result = new LinkedHashSet<UUID>();
    try {
      for (String value : values) {
        result.add(UUID.fromString(value));
      }
    } catch (IllegalArgumentException invalid) {
      throw new InvalidRequestException("JDG-V15", "Invalid metrics topics");
    }
    return Set.copyOf(result);
  }

  record Response(
      int streakDays,
      Instant lastSubmittedAt,
      List<SubmissionActivityDay> dailyCounts,
      @JsonInclude(JsonInclude.Include.NON_NULL) List<TopicMetric> acRateByTopic) {}
}
