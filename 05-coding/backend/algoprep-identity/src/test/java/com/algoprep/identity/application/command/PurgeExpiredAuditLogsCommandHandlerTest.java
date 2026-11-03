package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import org.junit.jupiter.api.Test;

/** JOB-ID-05 / BR-20: batches, the per-run cap and the policy. */
class PurgeExpiredAuditLogsCommandHandlerTest {
  private static final int SIZE = PurgeExpiredAuditLogsCommandHandler.BATCH_SIZE;
  private final LogRetentionRepository retention = mock(LogRetentionRepository.class);
  private final PurgeExpiredAuditLogsCommandHandler handler =
      new PurgeExpiredAuditLogsCommandHandler(retention);

  private void policy(ExpiredLogPolicy policy) {
    when(retention.read()).thenReturn(new LogRetentionSettings(45, policy));
  }

  @Test
  void aShortBatchEndsTheRun() {
    policy(ExpiredLogPolicy.DELETE);
    when(retention.purgeExpiredBatch(45, SIZE)).thenReturn(SIZE, 7);

    assertThat(handler.purge()).isEqualTo(SIZE + 7);
    verify(retention, times(2)).purgeExpiredBatch(45, SIZE);
  }

  @Test
  void nothingExpiredCostsOneBatch() {
    policy(ExpiredLogPolicy.DELETE);
    when(retention.purgeExpiredBatch(45, SIZE)).thenReturn(0);

    assertThat(handler.purge()).isZero();
    verify(retention, times(1)).purgeExpiredBatch(45, SIZE);
  }

  @Test
  void aBacklogStopsAtTheBatchCapAndWaitsForTheNextRun() {
    policy(ExpiredLogPolicy.DELETE);
    when(retention.purgeExpiredBatch(45, SIZE)).thenReturn(SIZE);

    assertThat(handler.purge()).isEqualTo(SIZE * PurgeExpiredAuditLogsCommandHandler.MAX_BATCHES);
    verify(retention, times(PurgeExpiredAuditLogsCommandHandler.MAX_BATCHES))
        .purgeExpiredBatch(45, SIZE);
  }

  @Test
  void archivePolicyKeepsEveryRow() {
    policy(ExpiredLogPolicy.ARCHIVE);

    assertThat(handler.purge()).isZero();
    verify(retention, never()).purgeExpiredBatch(anyInt(), anyInt());
  }
}
