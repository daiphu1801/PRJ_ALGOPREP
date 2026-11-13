package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real WebSocket/STOMP frames, with bounded waits and no synthetic broker messages. */
final class StompWireClient implements WebSocket.Listener, AutoCloseable {
  private static final ConcurrentMap<String, CompletableFuture<Void>> SUBSCRIPTIONS =
      new ConcurrentHashMap<>();
  private final String subscriptionId = java.util.UUID.randomUUID().toString();
  private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
  private final StringBuilder pending = new StringBuilder();
  private final CompletableFuture<Integer> closed = new CompletableFuture<>();
  private WebSocket socket;

  static StompWireClient open(int port) throws Exception {
    StompWireClient client = new StompWireClient();
    client.socket =
        HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .subprotocols("v12.stomp")
            .buildAsync(URI.create("ws://localhost:" + port + "/ws"), client)
            .get(5, TimeUnit.SECONDS);
    return client;
  }

  void connect(String token) throws Exception {
    send(
        "CONNECT\naccept-version:1.2\nhost:localhost\nheart-beat:0,0\n"
            + (token == null ? "" : "Authorization:Bearer " + token + "\n")
            + "\n");
  }

  void subscribe(String destination) throws Exception {
    SUBSCRIPTIONS.put(subscriptionId, new CompletableFuture<>());
    send("SUBSCRIBE\nid:" + subscriptionId + "\ndestination:" + destination + "\n\n");
  }

  void subscribed(String destination) throws Exception {
    subscribe(destination);
    SUBSCRIPTIONS.get(subscriptionId).get(5, TimeUnit.SECONDS);
  }

  static void brokerHandled(String id) {
    CompletableFuture<Void> future = SUBSCRIPTIONS.get(id);
    if (future != null) {
      future.complete(null);
    }
  }

  void send(String frame) throws Exception {
    socket.sendText(frame + '\0', true).get(5, TimeUnit.SECONDS);
  }

  String next() throws Exception {
    String frame = frames.poll(5, TimeUnit.SECONDS);
    assertThat(frame).as("Expected a STOMP frame; socket closed=%s", closed.isDone()).isNotNull();
    return frame;
  }

  void deniedAndClosed() throws Exception {
    assertThat(next()).isEqualTo("ERROR\nmessage:Request denied\ncontent-length:0\n\n");
    assertThat(closed.get(5, TimeUnit.SECONDS)).isNotNull();
  }

  void noMessage() throws Exception {
    assertThat(frames.poll(250, TimeUnit.MILLISECONDS)).isNull();
  }

  int awaitClose() throws Exception {
    return closed.get(6, TimeUnit.SECONDS);
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
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    closed.complete(statusCode);
    return null;
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    closed.completeExceptionally(error);
  }

  @Override
  public void close() {
    SUBSCRIPTIONS.remove(subscriptionId);
    socket.abort();
  }
}
