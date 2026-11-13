package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.SampleSettings.Effective;
import java.util.List;
import java.util.UUID;

public record ExecutionLimitsView(UUID problemId, List<Effective> limits) {
  public ExecutionLimitsView {
    limits = List.copyOf(limits);
  }
}
