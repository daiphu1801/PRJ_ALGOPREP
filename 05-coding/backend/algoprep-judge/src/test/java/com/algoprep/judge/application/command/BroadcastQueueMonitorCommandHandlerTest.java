package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.QueueMonitorBatch;
import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import com.algoprep.judge.application.ports.out.QueueMonitorAudiencePort;
import com.algoprep.judge.application.ports.out.QueueMonitorBroadcastPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BroadcastQueueMonitorCommandHandlerTest {
  private final QueueMonitorAudiencePort audience = mock(QueueMonitorAudiencePort.class);
  private final GetQueueMonitorBatchUseCase batches = mock(GetQueueMonitorBatchUseCase.class);
  private final QueueMonitorBroadcastPort broadcast = mock(QueueMonitorBroadcastPort.class);
  private final BroadcastQueueMonitorCommandHandler handler =
      new BroadcastQueueMonitorCommandHandler(audience, batches, broadcast);
  private final QueueMonitorBatch batch =
      new QueueMonitorBatch("QUEUE_BATCH", Instant.EPOCH, null, null, null);

  @Test
  void idleAudienceDoesNotReadDatabaseOrPublish() {
    handler.broadcast();
    verifyNoInteractions(batches, broadcast);
  }

  @Test
  void oneReadProducesOneBroadcastRegardlessOfAudienceSize() {
    when(audience.hasSubscribers()).thenReturn(true);
    when(batches.read()).thenReturn(batch);
    handler.broadcast();
    verify(batches).read();
    verify(broadcast).publish(batch);
  }

  @Test
  void lastUnsubscribeDuringReadSuppressesTheStaleBroadcast() {
    when(audience.hasSubscribers()).thenReturn(true, false);
    when(batches.read()).thenReturn(batch);
    handler.broadcast();
    verify(batches).read();
    verifyNoInteractions(broadcast);
  }

  @Test
  void failedReadNeverPublishesAnEmptyBatchAndTheNextTickRecovers() {
    when(audience.hasSubscribers()).thenReturn(true);
    when(batches.read())
        .thenThrow(new IllegalStateException("private SQL cause"))
        .thenReturn(batch);
    assertThatCode(handler::broadcast).doesNotThrowAnyException();
    verifyNoInteractions(broadcast);
    handler.broadcast();
    verify(broadcast).publish(batch);
  }

  @Test
  void brokerAndAudienceFailuresDoNotEscapeIntoOtherJobs() {
    when(audience.hasSubscribers())
        .thenThrow(new IllegalStateException("private token"))
        .thenReturn(true);
    assertThatCode(handler::broadcast).doesNotThrowAnyException();
    verifyNoInteractions(batches, broadcast);
    when(batches.read()).thenReturn(batch);
    doThrow(new IllegalStateException("private transport cause")).when(broadcast).publish(batch);
    assertThatCode(handler::broadcast).doesNotThrowAnyException();
  }
}
