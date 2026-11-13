package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.SolveState;
import java.util.Map;
import java.util.UUID;

/** Personal progress read #48 (at most 100 ids). */
public interface ListMyProblemSolveStatesUseCase {
  Map<UUID, SolveState> list(UUID userId, String problemIds);
}
