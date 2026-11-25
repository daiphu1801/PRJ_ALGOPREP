package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.execution.WorkerScaling;
import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.execution.WorkerControl;
import com.algoprep.judge.domain.model.execution.WorkerReport;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/** The worker publishes its consumers and the last scaling event for the ADM0401 snapshot. */
class WorkerStatusReportTest {
  private final WorkerJobRepository jobs = mock(WorkerJobRepository.class);
  private final WorkerLeasePort leases = mock(WorkerLeasePort.class);
  private final SimpleMessageListenerContainer container =
      mock(SimpleMessageListenerContainer.class);

  /** One poll is enough to move the target, so each scale() call is one scaling step. */
  private WorkerConsumptionControl started() {
    when(leases.acquire(anyString())).thenReturn(Optional.of(mock(Lease.class)));
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 0));
    var control = new WorkerConsumptionControl(jobs, leases, container, new WorkerScaling(8, 1, 1));
    control.start();
    return control;
  }

  private WorkerReport lastReport() {
    var captor = ArgumentCaptor.forClass(WorkerReport.class);
    verify(jobs, org.mockito.Mockito.atLeastOnce()).report(captor.capture());
    return captor.getAllValues().get(captor.getAllValues().size() - 1);
  }

  @Test
  void aPollThatChangesNothingReportsTargetAndActiveWithoutAScalingEvent() {
    when(container.getActiveConsumerCount()).thenReturn(1);
    var control = started();
    control.scale();
    var report = lastReport();
    assertThat(report.target()).isEqualTo(1);
    assertThat(report.active()).isEqualTo(1);
    assertThat(report.scale()).isNull();
  }

  @Test
  void aBacklogRaisesTheTargetAndReportsTheEventWithThePendingCount() {
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 22));
    when(leases.acquire(anyString())).thenReturn(Optional.of(mock(Lease.class)));
    when(container.getActiveConsumerCount()).thenReturn(1);
    var control = new WorkerConsumptionControl(jobs, leases, container, new WorkerScaling(8, 1, 1));
    control.start();
    control.scale();
    var report = lastReport();
    assertThat(report.target()).isEqualTo(2);
    assertThat(report.active()).isEqualTo(1);
    assertThat(report.scale()).isEqualTo(new WorkerReport.ScaleEvent(1, 2, "BACKLOG", 22));
  }

  @Test
  void anEmptyQueueLowersTheTargetAsIdleAndDisablingAutoscaleReportsDisabled() {
    var control = started();
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 22));
    control.scale();
    control.scale();
    assertThat(lastReport().scale().to()).isEqualTo(3);
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 0));
    control.scale();
    assertThat(lastReport().scale()).isEqualTo(new WorkerReport.ScaleEvent(3, 2, "IDLE", 0));
    when(jobs.control()).thenReturn(new WorkerControl(false, false, 20, 0));
    control.scale();
    assertThat(lastReport().scale()).isEqualTo(new WorkerReport.ScaleEvent(2, 1, "DISABLED", 0));
  }

  @Test
  void aFailedReportIsLoggedAndNeverStopsScalingOrMarksTheWorkerUnhealthy() {
    var control = started();
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 22));
    doThrow(new WorkerPersistenceException()).when(jobs).report(any());
    control.scale();
    assertThat(control.targetConsumers()).isEqualTo(2);
    assertThat(control.ready()).isTrue();
    verify(container).setConcurrency("2");
    control.scale();
    assertThat(control.targetConsumers()).isEqualTo(3);
  }

  @Test
  void aWorkerThatIsNotTheLeaderReportsNothing() {
    when(leases.acquire(anyString())).thenReturn(Optional.empty());
    var control = new WorkerConsumptionControl(jobs, leases, container, new WorkerScaling(8, 1, 1));
    org.assertj.core.api.Assertions.assertThatThrownBy(control::start)
        .isInstanceOf(IllegalStateException.class);
    control.scale();
    verify(jobs, never()).report(any());
  }
}
