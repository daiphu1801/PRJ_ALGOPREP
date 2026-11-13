package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.in.GetInfraSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetQueueSnapshotUseCase;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import com.algoprep.judge.domain.model.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.QueueMonitoring.JobSettings;
import com.algoprep.judge.domain.model.QueueMonitoring.Snapshot;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

class GetQueueMonitorBatchQueryHandlerTest {
  @Test
  void combinesExistingContractsWithoutLeakingJobItemsOrSummaryWindow() {
    var snapshots = mock(GetQueueSnapshotUseCase.class);
    var queue = mock(QueueMonitorRepository.class);
    var infra = mock(GetInfraSummaryUseCase.class);
    var now = Instant.parse("2026-11-05T00:00:00Z");
    var state = new Snapshot(now, "NORMAL", 1, false, true, 20, null, 10, 8);
    var settings = new JobSettings(20, 24, now);
    when(snapshots.read()).thenReturn(state);
    when(queue.jobSettings()).thenReturn(settings);
    when(queue.counts(settings)).thenReturn(new Counts(1, 2, 3));
    when(infra.summary()).thenReturn(new Summary(4, 5, 24));
    var batch = new GetQueueMonitorBatchQueryHandler(snapshots, queue, infra).read();
    assertThat(batch.type()).isEqualTo("QUEUE_BATCH");
    assertThat(batch.generatedAt()).isEqualTo(now);
    assertThat(batch.snapshot()).isEqualTo(state);
    assertThat(batch.jobCounts()).isEqualTo(new Counts(1, 2, 3));
    assertThat(batch.infraSummary().warnCount()).isEqualTo(4);
    assertThat(batch.infraSummary().errorCount()).isEqualTo(5);
    assertThat(JsonMapper.builder().build().writeValueAsString(batch))
        .doesNotContain("windowHours", "items", "source", "testcase", "workerId");
    verify(queue).counts(settings);
  }

  @Test
  void allReadsJoinOneReadOnlyRepeatableReadTransaction() throws Exception {
    var annotation =
        GetQueueMonitorBatchQueryHandler.class.getMethod("read").getAnnotation(Transactional.class);
    assertThat(annotation.readOnly()).isTrue();
    assertThat(annotation.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
  }
}
