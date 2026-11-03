package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.InfraEventQuery;
import com.algoprep.judge.domain.model.InfraEvents.Page;

public interface ListInfraEventsUseCase {
  Page list(InfraEventQuery query);
}
