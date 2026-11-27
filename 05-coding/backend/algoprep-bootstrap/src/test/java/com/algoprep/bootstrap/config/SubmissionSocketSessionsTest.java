package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class SubmissionSocketSessionsTest {
  private final ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
  private final SubmissionSocketSessions sessions = new SubmissionSocketSessions(scheduler);

  @Test
  void expiryIsScheduledOnceAndCancelledOnDisconnect() {
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn("id");
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    org.mockito.Mockito.doReturn(future)
        .when(scheduler)
        .schedule(any(Runnable.class), any(Instant.class));
    sessions.opened(socket);
    Instant expiry = Instant.now().plusSeconds(60);
    sessions.expireAdmin("id", expiry);
    sessions.expireAdmin("id", expiry);
    verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    sessions.closed("id");
    verify(future).cancel(false);
    sessions.closed("id");
  }

  @Test
  void expiryClosesThePhysicalSessionAndMissingOrExpiredSessionIsRejected() throws Exception {
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn("id");
    sessions.opened(socket);
    sessions.expireAdmin("id", Instant.now().plusSeconds(60));
    var task = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(task.capture(), any(Instant.class));
    task.getValue().run();
    verify(socket).close(CloseStatus.POLICY_VIOLATION);
    assertThatThrownBy(() -> sessions.expireAdmin("missing", Instant.now().plusSeconds(60)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sessions.expireAdmin("id", Instant.now().minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
