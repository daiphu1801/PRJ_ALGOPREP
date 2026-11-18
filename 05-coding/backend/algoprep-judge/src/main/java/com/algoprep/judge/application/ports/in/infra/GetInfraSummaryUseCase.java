package com.algoprep.judge.application.ports.in.infra;

import com.algoprep.judge.domain.model.infra.InfraEvents.Summary;

public interface GetInfraSummaryUseCase {
  Summary summary();
}
