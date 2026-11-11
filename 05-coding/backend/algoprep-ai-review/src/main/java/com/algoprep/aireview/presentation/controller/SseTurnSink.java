package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.TurnAccepted;
import com.algoprep.aireview.application.dto.TurnCompleted;
import com.algoprep.aireview.application.ports.out.InterviewTurnSink;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the turn events of DD api F5 section 2.2 to one {@link SseEmitter}. {@code seq} is
 * strictly increasing across data events; a keep-alive comment carries none. The first failed write
 * (or a container error callback) to a dead client marks the sink closed and completes {@link
 * #disconnected()}: the turn cancels the LLM stream and commits nothing
 * (DEC-2026-1110-f5-turn-budget-gate-and-cancel-on-disconnect); later events are dropped. A
 * ReentrantLock rather than {@code synchronized} so a slow write never pins a virtual thread.
 */
final class SseTurnSink implements InterviewTurnSink {
  private static final Logger LOG = Logger.getLogger(SseTurnSink.class.getName());
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final SseEmitter emitter;
  private final CompletableFuture<Void> opened = new CompletableFuture<>();
  private final CompletableFuture<Void> gone = new CompletableFuture<>();
  private final ReentrantLock lock = new ReentrantLock();
  private int seq;
  private boolean closed;

  SseTurnSink(SseEmitter emitter) {
    this.emitter = emitter;
    emitter.onCompletion(this::markClosed);
    emitter.onError(failure -> markClosed());
    emitter.onTimeout(this::markClosed);
  }

  /** Completes once {@code turn.accepted} is out, or exceptionally with the pre-stream failure. */
  CompletableFuture<Void> opened() {
    return opened;
  }

  @Override
  public void accepted(TurnAccepted event) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("sessionId", event.sessionId());
    data.put("userTurnIndex", event.userTurnIndex());
    data.put("aiTurnIndex", event.aiTurnIndex());
    data.put("stage", event.stage());
    data.put("turnCount", event.turnCount());
    data.put("maxTurns", event.maxTurns());
    send("turn.accepted", data);
    opened.complete(null);
  }

  @Override
  public void token(String text) {
    send("token", Map.of("text", text));
  }

  @Override
  public void completed(TurnCompleted event) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("aiTurnIndex", event.aiTurnIndex());
    data.put("content", event.content());
    data.put("stage", event.stage());
    data.put("stageChanged", event.stageChanged());
    data.put("turnCount", event.turnCount());
    data.put("maxTurns", event.maxTurns());
    data.put("shouldComplete", event.shouldComplete());
    send("turn.completed", data);
  }

  @Override
  public void failed(RuntimeException failure) {
    send("error", Map.of("error", envelope(failure)));
  }

  @Override
  public CompletableFuture<Void> disconnected() {
    return gone;
  }

  /** The keep-alive comment line; keeps a silent stream open through proxies. */
  void keepAlive() {
    lock.lock();
    try {
      if (!closed) {
        emitter.send(SseEmitter.event().comment("keep-alive"));
      }
    } catch (IOException | IllegalStateException dead) {
      closed = true;
      gone.complete(null);
    } finally {
      lock.unlock();
    }
  }

  void close() {
    lock.lock();
    try {
      closed = true;
      emitter.complete();
    } finally {
      lock.unlock();
    }
  }

  private void markClosed() {
    lock.lock();
    try {
      closed = true;
      gone.complete(null);
    } finally {
      lock.unlock();
    }
  }

  private void send(String name, Map<String, Object> fields) {
    lock.lock();
    try {
      int id = ++seq;
      if (closed) {
        return;
      }
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("seq", id);
      data.putAll(fields);
      emitter.send(
          SseEmitter.event()
              .name(name)
              .id(Integer.toString(id))
              .data(JSON.writeValueAsString(data)));
    } catch (IOException | IllegalStateException dead) {
      closed = true;
      gone.complete(null);
      LOG.log(Level.FINE, "Interview client went away; the turn is cancelled", dead);
    } finally {
      lock.unlock();
    }
  }

  /** RFC 9457 body of DD api section 1.1, as data of the {@code error} event. */
  static Map<String, Object> envelope(RuntimeException failure) {
    int status = 503;
    String title = "Service Unavailable";
    String code = AiProviderUnavailableException.CODE;
    Map<String, Object> details = Map.of("reason", "PROVIDER_ERROR");
    if (failure instanceof AiProviderUnavailableException unavailable) {
      details = unavailable.clientErrorDetails();
    } else if (failure instanceof AiInvalidResponseException invalid) {
      status = 502;
      title = "Bad Gateway";
      code = invalid.errorCode();
      details = null;
    } else if (failure instanceof AiSessionNotRunningException ended) {
      status = 409;
      title = "Conflict";
      code = ended.errorCode();
      details = null;
    } else if (failure instanceof AlgoPrepException known) {
      code = known.errorCode();
      details = failure instanceof ClientErrorDetails d ? d.clientErrorDetails() : null;
    } else {
      LOG.log(Level.WARNING, "Unexpected failure inside an interview turn", failure);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status);
    body.put("errorCode", code);
    if (details != null) {
      body.put("details", details);
    }
    return body;
  }
}
