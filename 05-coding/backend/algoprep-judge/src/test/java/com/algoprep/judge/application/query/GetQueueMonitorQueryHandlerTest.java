package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.QueueJobsQuery;
import com.algoprep.judge.domain.model.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.QueueMonitoring.Facts;
import com.algoprep.judge.domain.model.QueueMonitoring.Job;
import com.algoprep.judge.domain.model.QueueMonitoring.JobSettings;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetQueueMonitorQueryHandlerTest {
  private final QueueMonitorRepository repository = mock(QueueMonitorRepository.class);
  private final GetQueueMonitorQueryHandler handler =
      new GetQueueMonitorQueryHandler(repository, 8);

  @Test
  void snapshotUsesStrictBacklogThresholdPausePriorityAndNullableDelay() {
    when(repository.snapshot())
        .thenReturn(
            new Facts(Instant.EPOCH, false, true, 20, 0, null, 10),
            new Facts(Instant.EPOCH, false, true, 20, 20, 4000L, 10),
            new Facts(Instant.EPOCH, false, false, 20, 21, 4000L, 10),
            new Facts(Instant.EPOCH, true, false, 20, 21, 4000L, 3));
    var empty = handler.read();
    assertThat(empty.status()).isEqualTo("NORMAL");
    assertThat(empty.avgWaitMillis()).isNull();
    assertThat(empty.workerAutoscaleMax()).isEqualTo(8);
    assertThat(handler.read().status()).isEqualTo("NORMAL");
    assertThat(handler.read().status()).isEqualTo("BACKLOGGED");
    var paused = handler.read();
    assertThat(paused.status()).isEqualTo("PAUSED");
    assertThat(paused.queueConsumptionPaused()).isTrue();
    assertThat(paused.autoscaleEnabled()).isFalse();
    assertThat(paused.latencyCapSeconds()).isEqualTo(3);
  }

  @Test
  void countsSelectTheTabAndClampBeforeLoadingThePage() {
    var settings = new JobSettings(5, 24, Instant.EPOCH);
    var counts = new Counts(6, 1, 9);
    var job = new Job(UUID.randomUUID(), "JAVA", "PENDING", 100L);
    when(repository.jobSettings()).thenReturn(settings);
    when(repository.counts(settings)).thenReturn(counts);
    when(repository.jobs("waiting", settings, 5)).thenReturn(List.of(job));
    var result = handler.list(new QueueJobsQuery("waiting", 99));
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.pageSize()).isEqualTo(5);
    assertThat(result.totalCount()).isEqualTo(6);
    assertThat(result.counts()).isEqualTo(counts);
    assertThat(result.items()).containsExactly(job);
    when(repository.jobs("failed", settings, 0)).thenReturn(List.of());
    assertThat(handler.list(new QueueJobsQuery("failed", 99)).totalCount()).isEqualTo(1);
    when(repository.jobs("all", settings, 5)).thenReturn(List.of());
    assertThat(handler.list(new QueueJobsQuery("all", 99)).totalCount()).isEqualTo(9);
  }

  @Test
  void offsetArithmeticPreservesTheFullPositiveIntConfigurationRange() {
    var settings = new JobSettings(Integer.MAX_VALUE, Integer.MAX_VALUE, Instant.EPOCH);
    when(repository.jobSettings()).thenReturn(settings);
    when(repository.counts(settings)).thenReturn(new Counts(0, 0, Long.MAX_VALUE));
    when(repository.jobs("all", settings, 4_611_686_011_984_936_962L)).thenReturn(List.of());
    assertThat(handler.list(new QueueJobsQuery("all", Integer.MAX_VALUE)).page())
        .isEqualTo(Integer.MAX_VALUE);
    verify(repository).jobs("all", settings, 4_611_686_011_984_936_962L);
  }

  @Test
  void aConfiguredLimitThatDisagreesWithTheFixedRuntimeFailsFast() {
    for (int value : new int[] {0, 1, 7, 9, Integer.MAX_VALUE}) {
      assertThatThrownBy(
              () -> new GetQueueMonitorQueryHandler(repository, value).afterPropertiesSet())
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
