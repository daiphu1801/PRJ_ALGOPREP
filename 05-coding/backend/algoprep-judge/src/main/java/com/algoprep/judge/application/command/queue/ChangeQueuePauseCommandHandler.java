package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.QueuePauseChange;
import com.algoprep.judge.application.ports.in.ChangeQueuePauseUseCase;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChangeQueuePauseCommandHandler implements ChangeQueuePauseUseCase {
  private final JudgeMaintenanceRepository repository;

  ChangeQueuePauseCommandHandler(JudgeMaintenanceRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public QueuePauseChange change(boolean expectedPaused, boolean paused) {
    boolean current = repository.lockQueuePaused();
    if (current != expectedPaused) {
      return QueuePauseChange.CONFLICT;
    }
    if (current == paused) {
      return QueuePauseChange.UNCHANGED;
    }
    repository.changeQueuePaused(paused);
    if (!paused) {
      repository.refreshPendingHeartbeat();
    }
    return QueuePauseChange.CHANGED;
  }
}
