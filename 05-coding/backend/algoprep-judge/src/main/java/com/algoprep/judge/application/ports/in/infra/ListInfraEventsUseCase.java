package com.algoprep.judge.application.ports.in.infra;

import com.algoprep.judge.application.dto.infra.InfraEventQuery;
import com.algoprep.judge.domain.model.infra.InfraEvents.Page;

public interface ListInfraEventsUseCase {
  Page list(InfraEventQuery query);
}
