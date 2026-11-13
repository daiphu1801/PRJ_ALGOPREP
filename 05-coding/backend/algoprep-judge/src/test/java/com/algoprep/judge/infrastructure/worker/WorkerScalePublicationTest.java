package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.WorkerControl;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

class WorkerScalePublicationTest {
  @Test
  void targetIsVisibleInsideBothGrowingAndShrinkingTheContainer() {
    var fixture = fixture();
    doAnswer(
            invocation -> {
              int next = Integer.parseInt(invocation.getArgument(0));
              assertThat(fixture.control().targetConsumers()).isEqualTo(next);
              return null;
            })
        .when(fixture.container())
        .setConcurrency(anyString());
    fixture.control().scale();
    fixture.control().scale();
    assertThat(fixture.control().targetConsumers()).isEqualTo(2);
    when(fixture.jobs().control()).thenReturn(new WorkerControl(false, false, 20, 21));
    fixture.control().scale();
    assertThat(fixture.control().targetConsumers()).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void resizeFailureRollsBackThePublishedTargetAndPreservesCountersForRetry(boolean shrinking) {
    var fixture = fixture();
    fixture.control().scale();
    if (shrinking) {
      fixture.control().scale();
      when(fixture.jobs().control()).thenReturn(new WorkerControl(false, false, 20, 21));
    }
    int before = shrinking ? 2 : 1;
    int after = shrinking ? 1 : 2;
    var calls = new AtomicInteger();
    doAnswer(
            invocation -> {
              assertThat(fixture.control().targetConsumers()).isEqualTo(after);
              if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("resize rejected");
              }
              return null;
            })
        .when(fixture.container())
        .setConcurrency(Integer.toString(after));
    fixture.control().scale();
    assertThat(fixture.control().targetConsumers()).isEqualTo(before);
    assertThat(fixture.control().ready()).isFalse();
    fixture.control().scale();
    assertThat(calls.get()).isEqualTo(2);
    assertThat(fixture.control().targetConsumers()).isEqualTo(after);
  }

  private Fixture fixture() {
    var jobs = mock(WorkerJobRepository.class);
    var leases = mock(WorkerLeasePort.class);
    var container = mock(SimpleMessageListenerContainer.class);
    when(leases.acquire(anyString())).thenReturn(Optional.of(mock(WorkerLeasePort.Lease.class)));
    when(jobs.control()).thenReturn(new WorkerControl(false, true, 20, 21));
    var control = new WorkerConsumptionControl(jobs, leases, container);
    control.start();
    return new Fixture(jobs, container, control);
  }

  private record Fixture(
      WorkerJobRepository jobs,
      SimpleMessageListenerContainer container,
      WorkerConsumptionControl control) {}
}
