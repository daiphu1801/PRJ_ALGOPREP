package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.InfraEvents.Summary;

public interface GetInfraSummaryUseCase {
  Summary summary();
}
