package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Real network frames, with a broker barrier and enough time for the production five-second poll.
 */
final class QueueStreamWireClient implements WebSocket.Listener, AutoCloseable {
  private static final ConcurrentHashMap<String, CompletableFuture<Void>> OPERATIONS =
      new ConcurrentHashMap<>();
  private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
  private final StringBuilder pending = new StringBuilder();
  private final CompletableFuture<Integer> closed = new CompletableFuture<>();
  private WebSocket socket;

  static QueueStreamWireClient connect(int port, String token) throws Exception {
    var client = new QueueStreamWireClient();
    client.socket =
        HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .subprotocols("v12.stomp")
            .buildAsync(URI.create("ws://localhost:" + port + "/ws"), client)
            .get(10, TimeUnit.SECONDS);
    client.send(
        "CONNECT\naccept-version:1.2\nhost:localhost\nheart-beat:0,0\nAuthorization:Bearer "
            + token
            + "\n\n");
    assertThat(client.next()).startsWith("CONNECTED\n");
    return client;
  }

  void subscribe(String id) throws Exception {
    var future = new CompletableFuture<Void>();
    OPERATIONS.put(id, future);
    try {
      requestSubscribe(id);
      future.get(5, TimeUnit.SECONDS);
    } finally {
      OPERATIONS.remove(id);
    }
  }

  void requestSubscribe(String id) throws Exception {
    send("SUBSCRIBE\nid:" + id + "\ndestination:/topic/admin/queue\n\n");
  }

  void unsubscribe(String id) throws Exception {
    String key = "unsubscribe:" + id;
    var future = new CompletableFuture<Void>();
    OPERATIONS.put(key, future);
    try {
      send("UNSUBSCRIBE\nid:" + id + "\n\n");
      future.get(5, TimeUnit.SECONDS);
    } finally {
      OPERATIONS.remove(key);
    }
  }

  static void handled(String operation) {
    var future = OPERATIONS.get(operation);
    if (future != null) {
      future.complete(null);
    }
  }

  private void send(String frame) throws Exception {
    socket.sendText(frame + '\0', true).get(5, TimeUnit.SECONDS);
  }

  String next() throws Exception {
    String frame = frames.poll(10, TimeUnit.SECONDS);
    assertThat(frame).as("Expected STOMP frame; closed=%s", closed.isDone()).isNotNull();
    return frame;
  }

  void noMessage() throws Exception {
    assertThat(frames.poll(300, TimeUnit.MILLISECONDS)).isNull();
  }

  void denied() throws Exception {
    assertThat(next()).isEqualTo("ERROR\nmessage:Request denied\ncontent-length:0\n\n");
    assertThat(closed.get(5, TimeUnit.SECONDS)).isNotNull();
  }

  @Override
  public void onOpen(WebSocket webSocket) {
    webSocket.request(1);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    pending.append(data);
    int end;
    while ((end = pending.indexOf("\0")) >= 0) {
      frames.add(pending.substring(0, end).stripLeading());
      pending.delete(0, end + 1);
    }
    webSocket.request(1);
    return null;
  }

  @Override
  public CompletionStage<?> onClose(WebSocket socket, int status, String reason) {
    closed.complete(status);
    return null;
  }

  @Override
  public void onError(WebSocket socket, Throwable error) {
    closed.completeExceptionally(error);
  }

  @Override
  public void close() {
    socket.abort();
  }
}
