package com.algoprep.judge.application.dto;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import java.util.Set;
import java.util.UUID;

/** No identity argument: both HTTP and internal callers use the current STUDENT principal. */
public record MySubmissionMetricQuery(SubmissionMetricRange range, Set<UUID> topicIds) {
  public MySubmissionMetricQuery {
    if (range == null) {
      throw new InvalidRequestException("JDG-V07", "Invalid metrics range");
    }
    topicIds = Set.copyOf(topicIds);
    if (topicIds.size() > 50) {
      throw new InvalidRequestException("JDG-V15", "Invalid metrics topics");
    }
  }
}
