package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.application.dto.TurnAccepted;
import com.algoprep.aireview.application.dto.TurnCompleted;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE wire format of DD api F5 section 2.2 and the error envelope of section 1.1. */
class SseTurnSinkTest {
  private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  /** Records every event frame; optionally fails like a closed client connection. */
  private static final class RecordingEmitter extends SseEmitter {
    final List<String> frames = new ArrayList<>();
    boolean dead;
    boolean completed;

    RecordingEmitter() {
      super(0L);
    }

    @Override
    public void send(SseEventBuilder builder) throws IOException {
      if (dead) {
        throw new IOException("broken pipe");
      }
      StringBuilder frame = new StringBuilder();
      builder.build().forEach(part -> frame.append(part.getData()));
      frames.add(frame.toString());
    }

    @Override
    public synchronized void complete() {
      completed = true;
    }
  }

  @Test
  void eventsCarryStrictlyIncreasingSeqAndNamedFrames() {
    var emitter = new RecordingEmitter();
    var sink = new SseTurnSink(emitter);

    sink.accepted(new TurnAccepted(SESSION, 3, 4, "EXPLAIN", 3, 8));
    sink.token("Hel");
    sink.token("lo");
    sink.completed(new TurnCompleted(4, "Hello", "CHALLENGE", true, 4, null, false));

    assertThat(sink.opened()).isCompleted();
    assertThat(emitter.frames).hasSize(4);
    assertThat(emitter.frames.get(0))
        .contains("event:turn.accepted")
        .contains("\"seq\":1")
        .contains("\"userTurnIndex\":3")
        .contains("\"maxTurns\":8");
    assertThat(emitter.frames.get(1)).contains("event:token").contains("\"seq\":2").contains("Hel");
    assertThat(emitter.frames.get(3))
        .contains("event:turn.completed")
        .contains("\"seq\":4")
        .contains("\"stageChanged\":true")
        .contains("\"shouldComplete\":false");
  }

  @Test
  void keepAliveCommentCarriesNoSeqAndStopsAfterClose() {
    var emitter = new RecordingEmitter();
    var sink = new SseTurnSink(emitter);

    sink.keepAlive();
    sink.close();
    sink.keepAlive();
    sink.token("late");

    assertThat(emitter.frames).hasSize(1);
    assertThat(emitter.frames.get(0)).contains("keep-alive").doesNotContain("seq");
    assertThat(emitter.completed).isTrue();
  }

  @Test
  void failedWriteMarksDisconnectedAndDropsLaterEvents() {
    var emitter = new RecordingEmitter();
    var sink = new SseTurnSink(emitter);
    emitter.dead = true;

    sink.token("x");

    assertThat(sink.disconnected()).isCompleted();
    emitter.dead = false;
    sink.token("after");
    assertThat(emitter.frames).isEmpty();
  }

  @Test
  void failedKeepAliveAlsoMarksDisconnected() {
    var emitter = new RecordingEmitter();
    var sink = new SseTurnSink(emitter);
    emitter.dead = true;

    sink.keepAlive();

    assertThat(sink.disconnected()).isCompleted();
  }

  @Test
  void containerCallbacksMarkTheSinkClosed() throws Exception {
    for (String callback : List.of("completion", "timeout", "error")) {
      var emitter = new SseEmitter(0L);
      var sink = new SseTurnSink(emitter);
      var captured = new java.util.HashMap<String, Object>();
      Method initialize = null;
      for (Method m :
          org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.class
              .getDeclaredMethods()) {
        if (m.getName().equals("initialize")) {
          initialize = m;
        }
      }
      Class<?> handlerType = initialize.getParameterTypes()[0];
      Object handler =
          Proxy.newProxyInstance(
              handlerType.getClassLoader(),
              new Class<?>[] {handlerType},
              (p, m, a) -> {
                if (m.getName().startsWith("on")) {
                  captured.put(m.getName(), a[0]);
                }
                return null;
              });
      initialize.setAccessible(true);
      initialize.invoke(emitter, handler);

      switch (callback) {
        case "completion" -> ((Runnable) captured.get("onCompletion")).run();
        case "timeout" -> ((Runnable) captured.get("onTimeout")).run();
        default ->
            ((java.util.function.Consumer<Throwable>) captured.get("onError"))
                .accept(new IOException("reset"));
      }

      assertThat(sink.disconnected()).as(callback).isCompleted();
    }
  }

  @Test
  void failedWritesTheErrorEventWithEnvelope() {
    var emitter = new RecordingEmitter();
    var sink = new SseTurnSink(emitter);

    sink.failed(
        new AiProviderUnavailableException(AiProviderUnavailableException.Reason.TIMEOUT, null));

    assertThat(emitter.frames.get(0))
        .contains("event:error")
        .contains("\"status\":503")
        .contains("AIR-501")
        .contains("TIMEOUT");
  }

  @Test
  void envelopeMapsEachKnownFailureWithoutLeakingInternals() {
    Map<String, Object> provider =
        SseTurnSink.envelope(
            new AiProviderUnavailableException(
                AiProviderUnavailableException.Reason.UPSTREAM_PORT,
                new RuntimeException("secret")));
    assertThat(provider)
        .containsEntry("status", 503)
        .containsEntry("errorCode", "AIR-501")
        .containsEntry("details", Map.of("reason", "UPSTREAM_PORT"));

    Map<String, Object> invalid = SseTurnSink.envelope(new AiInvalidResponseException("bad json"));
    assertThat(invalid).containsEntry("status", 502).containsEntry("errorCode", "AIR-502");
    assertThat(invalid).doesNotContainKey("details");

    Map<String, Object> ended = SseTurnSink.envelope(new AiSessionNotRunningException());
    assertThat(ended).containsEntry("status", 409).containsEntry("errorCode", "AIR-111");

    Map<String, Object> known = SseTurnSink.envelope(new AiSessionNotFoundException());
    assertThat(known).containsEntry("errorCode", "AIR-110");

    Map<String, Object> unknown = SseTurnSink.envelope(new IllegalStateException("db password"));
    assertThat(unknown).containsEntry("errorCode", "AIR-501").containsEntry("status", 503);
    assertThat(unknown.toString()).doesNotContain("db password");
    assertThat(unknown).containsEntry("details", Map.of("reason", "PROVIDER_ERROR"));
  }
}
