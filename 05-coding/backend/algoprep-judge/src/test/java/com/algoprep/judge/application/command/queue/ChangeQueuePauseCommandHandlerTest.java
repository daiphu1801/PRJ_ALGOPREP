package com.algoprep.judge.application.command.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.queue.QueuePauseChange;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import org.junit.jupiter.api.Test;

class ChangeQueuePauseCommandHandlerTest {
  private final JudgeMaintenanceRepository repository = mock(JudgeMaintenanceRepository.class);

  @Test
  void resumeRefreshesOnlyOnARealPausedToOpenTransition() {
    var handler = new ChangeQueuePauseCommandHandler(repository);
    when(repository.lockQueuePaused()).thenReturn(true, false, false);
    assertThat(handler.change(true, false)).isEqualTo(QueuePauseChange.CHANGED);
    assertThat(handler.change(false, false)).isEqualTo(QueuePauseChange.UNCHANGED);
    assertThat(handler.change(true, false)).isEqualTo(QueuePauseChange.CONFLICT);
    verify(repository).refreshPendingHeartbeat();
    verify(repository).changeQueuePaused(false);
  }

  @Test
  void pauseDoesNotRewritePendingHeartbeat() {
    assertThat(new ChangeQueuePauseCommandHandler(repository).change(false, true))
        .isEqualTo(QueuePauseChange.CHANGED);
    verify(repository, never()).refreshPendingHeartbeat();
  }
}
