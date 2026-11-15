package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.SolveState;
import java.util.Map;
import java.util.UUID;

/** In-process read for other modules (no HTTP route): every problem the caller has submitted to. */
public interface ListAllMySolveStatesUseCase {
  Map<UUID, SolveState> listAll(UUID userId);
}
