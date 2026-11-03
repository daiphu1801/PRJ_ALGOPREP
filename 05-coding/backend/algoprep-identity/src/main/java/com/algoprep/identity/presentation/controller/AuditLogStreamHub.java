package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.dto.AuditLogItem;
import com.algoprep.identity.application.dto.AuditStreamBatch;
import com.algoprep.identity.application.dto.AuditStreamRequest;
import com.algoprep.identity.application.ports.in.StreamSystemAuditLogUseCase;
import com.algoprep.identity.domain.exception.AuditReadErrorCodes;
import com.algoprep.identity.domain.model.AuditLogCursor;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Fans new audit rows out to every open live stream (#66) from one shared poller.
 *
 * <p>Rows are written by other transactions of the same application, so there is nothing to
 * subscribe to; one thread asks the database for rows after the last position it delivered and
 * sends them to every emitter. The poller runs only while somebody is connected. A heartbeat
 * comment keeps proxies open and lets a vanished client surface as a failed write, which removes
 * its emitter. As an {@link IntSupplier} the hub also reports how many viewers are connected (for
 * tests and operations).
 *
 * <p>ponytail: single instance only. A second backend instance polls independently and each client
 * sees the rows from the instance it is connected to, which is correct because all instances read
 * the same table, just not shared work. Ceiling: {@code maxSubscribers} viewers and about {@value
 * #BATCH} rows per poll interval (50 rows/s at the default 2 s); beyond that the stream lags and a
 * client that reconnects gets a {@code resync}. A row committed late with an older {@code
 * created_at} than one already delivered is missed by the live feed (it still shows on reload);
 * upgrade path is a lookback window with an id de-duplication set, or PostgreSQL LISTEN/NOTIFY.
 * Sends happen under one lock, so one stalled client can delay the others until its write times
 * out; the lock is a {@link ReentrantLock} so waiting for it never pins a virtual thread.
 */
@Component("auditStreamSubscribers")
class AuditLogStreamHub implements IntSupplier {
  static final String EVENT = "audit-log";
  static final String RESYNC = "resync";
  static final int BATCH = 100;
  private static final int CATCH_UP = 200;
  private static final AuditLogCursor EMPTY_LOG = new AuditLogCursor(Instant.EPOCH, new UUID(0, 0));
  private static final Logger LOG = LoggerFactory.getLogger(AuditLogStreamHub.class);

  private final StreamSystemAuditLogUseCase stream;
  private final Duration pollInterval;
  private final Duration heartbeatInterval;
  private final Duration timeout;
  private final int maxSubscribers;

  // ReentrantLock, not synchronized: a blocked or slow send must not pin a virtual thread's
  // carrier.
  private final ReentrantLock lock = new ReentrantLock();
  private final Set<SseEmitter> emitters = new LinkedHashSet<>();
  private AuditLogCursor watermark;
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> poller;
  private Instant lastHeartbeat = Instant.now();

  AuditLogStreamHub(
      StreamSystemAuditLogUseCase stream,
      @Value("${algoprep.identity.audit-stream.poll-interval:2s}") Duration pollInterval,
      @Value("${algoprep.identity.audit-stream.heartbeat-interval:30s}") Duration heartbeatInterval,
      @Value("${algoprep.identity.audit-stream.timeout:30m}") Duration timeout,
      @Value("${algoprep.identity.audit-stream.max-subscribers:50}") int maxSubscribers) {
    this.stream = stream;
    this.pollInterval = pollInterval;
    this.heartbeatInterval = heartbeatInterval;
    this.timeout = timeout;
    this.maxSubscribers = maxSubscribers;
  }

  /** Opens a stream; a reconnecting client passes its {@code Last-Event-ID}. */
  SseEmitter subscribe(String lastEventId) {
    UUID marker = parse(lastEventId);
    SseEmitter emitter = new SseEmitter(timeout.toMillis());
    locked(
        () -> {
          if (emitters.size() >= maxSubscribers) {
            throw new RateLimitExceededException(
                AuditReadErrorCodes.AUDIT_STREAM_CAPACITY, "Too many live viewers");
          }
          if (watermark == null) {
            AuditStreamBatch start = stream.next(new AuditStreamRequest(null, null, null, 1));
            watermark = start.position() == null ? EMPTY_LOG : start.position();
          }
          emitter.onCompletion(() -> remove(emitter));
          emitter.onTimeout(emitter::complete);
          emitter.onError(failure -> remove(emitter));
          if (marker != null) {
            catchUp(emitter, marker);
          }
          emitters.add(emitter);
          if (poller == null) {
            scheduler =
                Executors.newSingleThreadScheduledExecutor(
                    r -> {
                      Thread thread = new Thread(r, "audit-log-stream");
                      thread.setDaemon(true);
                      return thread;
                    });
            poller =
                scheduler.scheduleWithFixedDelay(
                    this::tick,
                    pollInterval.toMillis(),
                    pollInterval.toMillis(),
                    TimeUnit.MILLISECONDS);
          }
        });
    return emitter;
  }

  @Override
  public int getAsInt() {
    return locked(emitters::size);
  }

  @PreDestroy
  void shutdown() {
    locked(
        () -> {
          stopPoller();
        });
  }

  private void catchUp(SseEmitter emitter, UUID marker) {
    AuditStreamBatch missed =
        stream.next(new AuditStreamRequest(null, marker, watermark, CATCH_UP));
    if (missed.resync() || missed.items().size() >= CATCH_UP) {
      // The marker is gone, or too much was missed to replay: let the client reload page one.
      send(emitter, SseEmitter.event().name(RESYNC).data(""));
      return;
    }
    for (AuditLogItem item : missed.items()) {
      send(emitter, event(item));
    }
  }

  private void tick() {
    locked(
        () -> {
          if (emitters.isEmpty()) {
            return;
          }
          try {
            AuditStreamBatch batch =
                stream.next(new AuditStreamRequest(watermark, null, null, BATCH));
            // Advance first: a send that empties the hub resets the watermark, and that must win.
            watermark = batch.position();
            for (AuditLogItem item : batch.items()) {
              broadcast(() -> event(item));
            }
            Instant now = Instant.now();
            if (Duration.between(lastHeartbeat, now).compareTo(heartbeatInterval) >= 0) {
              lastHeartbeat = now;
              broadcast(() -> SseEmitter.event().comment("heartbeat"));
            }
          } catch (RuntimeException failure) {
            // Never let the scheduled task die; the next tick retries from the same position.
            LOG.warn("Audit stream poll failed: {}", failure.getClass().getSimpleName());
          }
        });
  }

  private static SseEmitter.SseEventBuilder event(AuditLogItem item) {
    return SseEmitter.event()
        .id(item.id().toString())
        .name(EVENT)
        .data(item, MediaType.APPLICATION_JSON);
  }

  /** A builder is consumed by one send, so every emitter gets a fresh one. */
  private void broadcast(Supplier<SseEmitter.SseEventBuilder> event) {
    for (SseEmitter emitter : List.copyOf(emitters)) {
      send(emitter, event.get());
    }
  }

  private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
    try {
      emitter.send(event);
    } catch (IOException | IllegalStateException gone) {
      remove(emitter);
      try {
        emitter.complete();
      } catch (RuntimeException alreadyClosed) {
        LOG.debug("Audit stream emitter was already closed");
      }
    }
  }

  private void remove(SseEmitter emitter) {
    locked(
        () -> {
          emitters.remove(emitter);
          if (emitters.isEmpty()) {
            stopPoller();
          }
        });
  }

  private void stopPoller() {
    if (poller != null) {
      poller.cancel(false);
      poller = null;
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
    watermark = null;
  }

  private void locked(Runnable action) {
    locked(
        () -> {
          action.run();
          return null;
        });
  }

  private <T> T locked(Supplier<T> action) {
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
    }
  }

  private static UUID parse(String lastEventId) {
    if (lastEventId == null || lastEventId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(lastEventId.trim());
    } catch (IllegalArgumentException malformed) {
      return new UUID(0, 0);
    }
  }
}
