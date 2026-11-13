package com.algoprep.bootstrap.config;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/** Owns physical sessions so an admin subscription cannot outlive its JWT. */
final class SubmissionSocketSessions {
  private final ConcurrentMap<String, Entry> sessions = new ConcurrentHashMap<>();
  private final ThreadPoolTaskScheduler scheduler;

  SubmissionSocketSessions(ThreadPoolTaskScheduler scheduler) {
    this.scheduler = scheduler;
  }

  void opened(WebSocketSession session) {
    sessions.put(session.getId(), new Entry(session));
  }

  void expireAdmin(String sessionId, Instant expiry) {
    Entry entry = sessions.get(sessionId);
    if (entry == null || !expiry.isAfter(Instant.now())) {
      throw new IllegalArgumentException("Request denied");
    }
    synchronized (entry) {
      if (entry.expiry == null) {
        entry.expiry = scheduler.schedule(() -> close(entry), expiry);
      }
    }
  }

  void closed(String sessionId) {
    Entry entry = sessions.remove(sessionId);
    if (entry != null) {
      synchronized (entry) {
        if (entry.expiry != null) {
          entry.expiry.cancel(false);
        }
      }
    }
  }

  private static void close(Entry entry) {
    try {
      entry.session.close(CloseStatus.POLICY_VIOLATION);
    } catch (IOException error) {
      // The peer may already have gone away; no token or transport exception is logged.
    }
  }

  private static final class Entry {
    private final WebSocketSession session;
    private ScheduledFuture<?> expiry;

    private Entry(WebSocketSession session) {
      this.session = session;
    }
  }
}
