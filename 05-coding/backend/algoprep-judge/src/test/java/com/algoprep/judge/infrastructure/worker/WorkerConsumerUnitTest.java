package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.WorkerScaling;
import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.algoprep.judge.domain.model.WorkerControl;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

class WorkerConsumerUnitTest {
  @Test
  void missingChannelCannotRunOrAcknowledgeWork() {
    var process = mock(ProcessQueuedSubmissionUseCase.class);
    assertThatThrownBy(() -> new WorkerMessageListener(process).onMessage(message("{}"), null))
        .isInstanceOf(IllegalStateException.class);
    org.mockito.Mockito.verifyNoInteractions(process);
  }

  @Test
  void failedConsumerResizeKeepsTheOldCountersAndRetries() {
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var container = mock(SimpleMessageListenerContainer.class);
    var lease = mock(Lease.class);
    when(leases.acquire(anyString())).thenReturn(Optional.of(lease));
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 21));
    var control = new WorkerConsumptionControl(jobs, leases, container);
    control.start();
    control.start();
    verify(leases).acquire("F4_GRADING_WORKER_LEADER");
    control.scale();
    org.mockito.Mockito.doThrow(new IllegalStateException("resize"))
        .doNothing()
        .when(container)
        .setConcurrency("2");
    control.scale();
    assertThat(control.ready()).isFalse();
    control.scale();
    verify(container, org.mockito.Mockito.times(2)).setConcurrency("2");
    control.stop();
  }

  static Message message(String text) {
    var properties = new MessageProperties();
    properties.setDeliveryTag(7);
    return new Message(text.getBytes(StandardCharsets.UTF_8), properties);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "null",
        "[]",
        "{bad",
        "{\"submissionId\":\"bad-id\"}",
        "{\"submissionId\":12}",
        "{\"submissionId\":\"00000000-0000-0000-0000-000000000001\",\"sourceCode\":\"private\"}",
        "{\"submissionId\":\"00000000-0000-0000-0000-000000000001\",\"submissionId\":\"00000000-0000-0000-0000-000000000002\"}",
        "{\"submissionId\":\"00000000-0000-0000-0000-000000000001\"}{}"
      })
  void poisonMessagesAreRejectedWithoutCallingTheApplication(String body) throws Exception {
    var process = mock(ProcessQueuedSubmissionUseCase.class);
    var channel = mock(Channel.class);
    new WorkerMessageListener(process).onMessage(message(body), channel);
    verify(channel).basicReject(7, false);
    org.mockito.Mockito.verifyNoInteractions(process);
  }

  @Test
  void ackFollowsTheApplicationAndPersistenceFailureRequeues() throws Exception {
    UUID id = UUID.randomUUID();
    var channel = mock(Channel.class);
    var process = mock(ProcessQueuedSubmissionUseCase.class);
    var listener = new WorkerMessageListener(process);
    listener.onMessage(message("{\"submissionId\":\"" + id + "\"}"), channel);
    var order = org.mockito.Mockito.inOrder(process, channel);
    order.verify(process).process(id);
    order.verify(channel).basicAck(7, false);
    var failing = mock(ProcessQueuedSubmissionUseCase.class);
    doThrow(new IllegalStateException("private source")).when(failing).process(id);
    var retry = mock(Channel.class);
    new WorkerMessageListener(failing)
        .onMessage(message("{\"submissionId\":\"" + id + "\"}"), retry);
    verify(retry).basicNack(7, false, true);
    verify(retry, never()).basicAck(7, false);
  }

  @Test
  void scaleUsesTwoHighAndFourEmptyCyclesWithBoundsAndPauseResets() {
    var scaling = new WorkerScaling();
    var high = new WorkerControl(false, true, 20, 21);
    assertThat(scaling.poll(high)).isEqualTo(1);
    assertThat(scaling.poll(new WorkerControl(true, true, 20, 21))).isEqualTo(1);
    assertThat(scaling.poll(high)).isEqualTo(1);
    assertThat(scaling.poll(high)).isEqualTo(2);
    for (int i = 0; i < 30; i++) {
      scaling.poll(high);
    }
    assertThat(scaling.target()).isEqualTo(8);
    var empty = new WorkerControl(false, true, 20, 0);
    for (int i = 0; i < 3; i++) {
      assertThat(scaling.poll(empty)).isEqualTo(8);
    }
    assertThat(scaling.poll(empty)).isEqualTo(7);
    assertThat(scaling.poll(new WorkerControl(false, false, 20, 99))).isEqualTo(1);
    assertThat(scaling.poll(new WorkerControl(false, true, 100, 99))).isEqualTo(1);
  }

  @Test
  void onlyTheLeaderStartsAndLostLeadershipCannotRestartTheListener() {
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var lease = mock(Lease.class);
    var container = mock(SimpleMessageListenerContainer.class);
    when(leases.acquire(anyString())).thenReturn(Optional.of(lease));
    when(jobs.control()).thenReturn(new WorkerControl(true, true, 20, 21));
    var control = new WorkerConsumptionControl(jobs, leases, container);
    control.start();
    verify(container, never()).start();
    assertThat(control.ready()).isTrue();
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 21));
    control.gate();
    verify(container).start();
    control.scale();
    control.scale();
    verify(container).setConcurrency("2");
    doThrow(new IllegalStateException("lost")).when(lease).check();
    control.gate();
    assertThat(control.ready()).isFalse();
    assertThat(control.isRunning()).isFalse();
    verify(lease).close();
    control.gate();
    verify(leases).acquire("F4_GRADING_WORKER_LEADER");
  }

  @Test
  void aStandbyDoesNotConsumeAndSettingsFailureKeepsTheLastTarget() {
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var container = mock(SimpleMessageListenerContainer.class);
    when(leases.acquire(anyString())).thenReturn(Optional.empty());
    var standby = new WorkerConsumptionControl(jobs, leases, container);
    assertThatThrownBy(standby::start).isInstanceOf(IllegalStateException.class);
    verify(container, never()).start();
    var lease = mock(Lease.class);
    when(leases.acquire(anyString())).thenReturn(Optional.of(lease));
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 0));
    var control = new WorkerConsumptionControl(jobs, leases, container);
    control.start();
    when(jobs.control()).thenThrow(new IllegalStateException("DB"));
    control.gate();
    control.scale();
    assertThat(control.isRunning()).isTrue();
    assertThat(control.ready()).isFalse();
    verify(lease, never()).close();
    control.stop();
    verify(lease).close();
  }
}
