package com.algoprep.identity.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.dto.AuditLogItem;
import com.algoprep.identity.application.dto.AuditStreamBatch;
import com.algoprep.identity.application.dto.AuditStreamRequest;
import com.algoprep.identity.application.ports.in.StreamSystemAuditLogUseCase;
import com.algoprep.identity.domain.model.AuditLogCursor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

/** Live audit stream (SSE): capacity limit, catch-up after reconnect, polling and shutdown. */
class AuditLogStreamHubTest {
  private final StreamSystemAuditLogUseCase stream = mock(StreamSystemAuditLogUseCase.class);
  private AuditLogStreamHub hub;
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");

  private AuditLogItem item() {
    return new AuditLogItem(
        UUID.randomUUID(), now, "A", "a@b.c", "LOGIN", "AUTH", "identity", "USER", null, Map.of());
  }

  private AuditLogStreamHub hub(int max, Duration poll) {
    hub = new AuditLogStreamHub(stream, poll, Duration.ofMillis(10), Duration.ofMinutes(1), max);
    return hub;
  }

  @AfterEach
  void stop() {
    if (hub != null) {
      hub.shutdown();
    }
  }

  @Test
  void subscribeStartsFromTheLatestRowAndCountsSubscribers() {
    when(stream.next(any()))
        .thenReturn(
            new AuditStreamBatch(List.of(), new AuditLogCursor(now, UUID.randomUUID()), false));
    var h = hub(2, Duration.ofHours(1));
    assertThat(h.getAsInt()).isZero();
    var emitter = h.subscribe(null);
    assertThat(emitter.getTimeout()).isEqualTo(Duration.ofMinutes(1).toMillis());
    assertThat(h.getAsInt()).isEqualTo(1);
    h.subscribe("  ");
    assertThat(h.getAsInt()).isEqualTo(2);
  }

  @Test
  void subscribeBeyondCapacityIsRejected() {
    when(stream.next(any())).thenReturn(new AuditStreamBatch(List.of(), null, false));
    var h = hub(1, Duration.ofHours(1));
    h.subscribe(null);
    assertThatThrownBy(() -> h.subscribe(null)).isInstanceOf(RateLimitExceededException.class);
  }

  @Test
  void reconnectWithKnownMarkerReplaysMissedRows() {
    var start = new AuditStreamBatch(List.of(), new AuditLogCursor(now, UUID.randomUUID()), false);
    var missed = new AuditStreamBatch(List.of(item(), item()), start.position(), false);
    when(stream.next(any())).thenReturn(start, missed);
    var h = hub(5, Duration.ofHours(1));
    var emitter = h.subscribe(UUID.randomUUID().toString());
    assertThat(emitter).isNotNull();
    assertThat(h.getAsInt()).isEqualTo(1);
  }

  @Test
  void reconnectWithMalformedMarkerOrResyncAsksTheClientToResync() {
    var start = new AuditStreamBatch(List.of(), new AuditLogCursor(now, UUID.randomUUID()), false);
    var resync = new AuditStreamBatch(List.of(), start.position(), true);
    when(stream.next(any())).thenReturn(start, resync, resync);
    var h = hub(5, Duration.ofHours(1));
    assertThat(h.subscribe("not-a-uuid")).isNotNull();
    assertThat(h.subscribe(UUID.randomUUID().toString())).isNotNull();
    assertThat(h.getAsInt()).isEqualTo(2);
  }

  @Test
  void pollerBroadcastsNewRowsToSubscribers() {
    AtomicInteger polls = new AtomicInteger();
    when(stream.next(any()))
        .thenAnswer(
            inv -> {
              AuditStreamRequest request = inv.getArgument(0);
              if (request.limit() == 1) {
                return new AuditStreamBatch(
                    List.of(), new AuditLogCursor(now, UUID.randomUUID()), false);
              }
              polls.incrementAndGet();
              return new AuditStreamBatch(
                  List.of(item()), new AuditLogCursor(now, UUID.randomUUID()), false);
            });
    var h = hub(5, Duration.ofMillis(20));
    h.subscribe(null);
    await().atMost(Duration.ofSeconds(5)).until(() -> polls.get() >= 2);
    assertThat(h.getAsInt()).isEqualTo(1);
  }

  @Test
  void pollFailureIsSwallowedAndPollingContinues() {
    AtomicInteger polls = new AtomicInteger();
    when(stream.next(any()))
        .thenAnswer(
            inv -> {
              AuditStreamRequest request = inv.getArgument(0);
              if (request.limit() == 1) {
                return new AuditStreamBatch(List.of(), null, false);
              }
              polls.incrementAndGet();
              throw new IllegalStateException("db down");
            });
    var h = hub(5, Duration.ofMillis(20));
    h.subscribe(null);
    await().atMost(Duration.ofSeconds(5)).until(() -> polls.get() >= 2);
  }

  @Test
  void controllerRequiresAdminAndSetsNoStoreHeader() {
    when(stream.next(any())).thenReturn(new AuditStreamBatch(List.of(), null, false));
    var h = hub(5, Duration.ofHours(1));
    var controller = new AuditLogStreamController(h);
    var response = new MockHttpServletResponse();
    var admin = new TestingAuthenticationToken("u", "n", "ROLE_ADMIN");
    assertThat(controller.stream(admin, null, response)).isNotNull();
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, no-transform");
    var student = new TestingAuthenticationToken("u", "n", "ROLE_STUDENT");
    assertThatThrownBy(() -> controller.stream(student, null, new MockHttpServletResponse()))
        .isInstanceOf(PermissionDeniedException.class);
  }
}
