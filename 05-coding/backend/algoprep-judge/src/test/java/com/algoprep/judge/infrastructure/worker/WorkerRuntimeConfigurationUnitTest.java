package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.algoprep.judge.domain.model.WorkerControl;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

class WorkerRuntimeConfigurationUnitTest {
  @Test
  void poolCannotStarveTheEighthJobAndQueueIsDurable() {
    var config = new WorkerRuntimeConfiguration();
    assertThat(config.gradingSubmissionQueue().isDurable()).isTrue();
    assertThatThrownBy(
            () ->
                config.gradingConsumptionControl(
                    mock(WorkerJobRepository.class),
                    mock(WorkerLeasePort.class),
                    mock(ConnectionFactory.class),
                    mock(ProcessQueuedSubmissionUseCase.class),
                    new WorkerRuntimeConfiguration.ScalingSettings(17, 8, 2, 4)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            config.gradingConsumptionControl(
                mock(WorkerJobRepository.class),
                mock(WorkerLeasePort.class),
                mock(ConnectionFactory.class),
                mock(ProcessQueuedSubmissionUseCase.class),
                new WorkerRuntimeConfiguration.ScalingSettings(18, 8, 2, 4)))
        .isNotNull();
  }

  @Test
  void poolGuardTracksConfiguredCapAndRejectsInvalidScalingValues() {
    var config = new WorkerRuntimeConfiguration();
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var connection = mock(ConnectionFactory.class);
    var process = mock(ProcessQueuedSubmissionUseCase.class);
    assertThat(
            config.gradingConsumptionControl(
                jobs,
                leases,
                connection,
                process,
                new WorkerRuntimeConfiguration.ScalingSettings(13, 3, 1, 2)))
        .isNotNull();
    assertThatThrownBy(
            () ->
                config.gradingConsumptionControl(
                    jobs,
                    leases,
                    connection,
                    process,
                    new WorkerRuntimeConfiguration.ScalingSettings(12, 3, 1, 2)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                config.gradingConsumptionControl(
                    jobs,
                    leases,
                    connection,
                    process,
                    new WorkerRuntimeConfiguration.ScalingSettings(
                        Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 2)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                config.gradingConsumptionControl(
                    jobs,
                    leases,
                    connection,
                    process,
                    new WorkerRuntimeConfiguration.ScalingSettings(48, 0, 1, 2)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void metersFollowActualConsumersAndHealthReflectsSettingsFailure() {
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var container = mock(SimpleMessageListenerContainer.class);
    when(leases.acquire(anyString())).thenReturn(Optional.of(mock(WorkerLeasePort.Lease.class)));
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 21));
    var control = new WorkerConsumptionControl(jobs, leases, container);
    var config = new WorkerRuntimeConfiguration();
    var health = config.gradingWorkerHealthIndicator(control);
    var registry = new SimpleMeterRegistry();
    try {
      config.gradingWorkerConsumerMetrics(control).bindTo(registry);
      assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
      assertThat(registry.get("judge.grading.consumers.target").gauge().value()).isEqualTo(1);
      control.start();
      control.scale();
      control.scale();
      when(container.getActiveConsumerCount()).thenReturn(2);
      assertThat(registry.get("judge.grading.consumers.target").gauge().value()).isEqualTo(2);
      assertThat(registry.get("judge.grading.consumers.active").gauge().value()).isEqualTo(2);
      assertThat(health.health().getStatus().getCode()).isEqualTo("UP");
      when(jobs.control()).thenThrow(new IllegalStateException("database"));
      control.gate();
      assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
      assertThat(registry.get("judge.grading.consumers.target").gauge().value()).isEqualTo(2);
      when(container.getActiveConsumerCount()).thenReturn(0);
      control.stop();
    } finally {
      registry.close();
    }
  }
}
