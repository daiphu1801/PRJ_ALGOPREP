package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.SendInterviewTurnCommand;
import com.algoprep.aireview.application.ports.in.SendInterviewTurnUseCase;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * DD api F5 section 10: the response of the POST is the {@code text/event-stream}. The use case
 * runs on its own virtual thread; the request thread only waits until {@code turn.accepted} was
 * sent (or the use case threw), so every failure before that is an ordinary RFC 9457 JSON error and
 * every failure after it is an SSE {@code error} event (DD 2.1). Spring MVC with virtual threads,
 * no WebFlux. ponytail: the live-turn set is per instance, like the reply tracker (DD 2.4).
 */
@RestController
@RequestMapping("/api/v1/ai/interview-sessions")
class InterviewTurnController implements DisposableBean {
  private final SendInterviewTurnUseCase turns;
  private final long keepAliveMillis;
  private final ScheduledExecutorService keepAlives =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "ai-interview-keepalive");
            thread.setDaemon(true);
            return thread;
          });

  InterviewTurnController(
      SendInterviewTurnUseCase turns,
      @Value("${ai.interview.keep-alive-millis:15000}") long keepAliveMillis) {
    this.turns = turns;
    this.keepAliveMillis = keepAliveMillis;
  }

  @PostMapping("/{sessionId}/turns")
  SseEmitter turn(
      JwtAuthenticationToken auth,
      @PathVariable String sessionId,
      @RequestBody InterviewTurnRequest body) {
    UUID id = sessionId(sessionId);
    SendInterviewTurnCommand command =
        body.toCommand(
            UUID.fromString(auth.getToken().getSubject()),
            InterviewSessionController.category(auth),
            id);
    SseEmitter emitter = new SseEmitter(0L);
    SseTurnSink sink = new SseTurnSink(emitter);
    ScheduledFuture<?> beat =
        keepAlives.scheduleAtFixedRate(
            sink::keepAlive, keepAliveMillis, keepAliveMillis, TimeUnit.MILLISECONDS);
    Thread.ofVirtual().name("ai-interview-turn").start(() -> run(command, sink, beat));
    try {
      sink.opened().get();
    } catch (ExecutionException failed) {
      beat.cancel(false);
      if (failed.getCause() instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException(failed.getCause());
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      beat.cancel(false);
      throw new IllegalStateException("interrupted while opening the turn stream", interrupted);
    }
    return emitter;
  }

  private void run(SendInterviewTurnCommand command, SseTurnSink sink, ScheduledFuture<?> beat) {
    try {
      turns.send(command, sink);
    } catch (RuntimeException failure) {
      if (!sink.opened().completeExceptionally(failure)) {
        sink.failed(failure);
      }
    } finally {
      // Never leave the request thread waiting if the use case ended without accepting.
      sink.opened().completeExceptionally(new IllegalStateException("turn ended before accept"));
      beat.cancel(false);
      sink.close();
    }
  }

  @Override
  public void destroy() {
    keepAlives.shutdownNow();
  }

  private static UUID sessionId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException invalid) {
      throw new AiSessionNotFoundException();
    }
  }
}
