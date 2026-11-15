package com.algoprep.identity.application.query.progress;

import com.algoprep.identity.application.ports.in.progress.ListAllMySolveStatesUseCase;
import com.algoprep.identity.domain.model.progress.SolveState;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feeds the problem list filter of problem-bank; the caller module owns caching (DD api F2 58.2).
 */
@Service
class ListAllMySolveStatesQueryHandler implements ListAllMySolveStatesUseCase {
  private final MyProgressReadRepository progress;

  ListAllMySolveStatesQueryHandler(MyProgressReadRepository progress) {
    this.progress = progress;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, SolveState> listAll(UUID userId) {
    return progress.allSolveStates(userId);
  }
}
