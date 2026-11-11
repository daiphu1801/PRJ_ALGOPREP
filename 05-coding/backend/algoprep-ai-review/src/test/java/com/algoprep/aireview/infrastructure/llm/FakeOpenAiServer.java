package com.algoprep.aireview.infrastructure.llm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local imitation of the OpenAI chat-completions API (JSON and SSE). No network beyond loopback:
 * tests never reach OpenRouter.
 */
final class FakeOpenAiServer implements AutoCloseable {
  /** What the fake saw. */
  record Seen(String path, String authorization, String body) {}

  private final HttpServer server;
  final List<Seen> requests = new CopyOnWriteArrayList<>();
  final AtomicBoolean clientDisconnected = new AtomicBoolean();

  volatile int status = 200;
  volatile String body = "";
  volatile long delayMillis;
  volatile boolean stream;
  volatile List<String> sseData = List.of();
  volatile long sseGapMillis;

  /** When true the stream never ends after the listed chunks. */
  volatile boolean sseHang;

  FakeOpenAiServer() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    server.createContext("/", this::handle);
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    server.start();
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private void handle(HttpExchange ex) throws IOException {
    String req = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    requests.add(
        new Seen(
            ex.getRequestURI().getPath(), ex.getRequestHeaders().getFirst("Authorization"), req));
    sleep(delayMillis);
    if (stream && status == 200) {
      ex.getResponseHeaders().add("Content-Type", "text/event-stream");
      ex.sendResponseHeaders(200, 0);
      try (OutputStream out = ex.getResponseBody()) {
        for (String data : sseData) {
          out.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
          out.flush();
          sleep(sseGapMillis);
        }
        if (sseHang) {
          while (true) {
            sleep(100);
            out.write(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
          }
        }
        out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
      } catch (IOException disconnect) {
        clientDisconnected.set(true);
      }
      return;
    }
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "application/json");
    ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    if (bytes.length > 0) {
      try (OutputStream out = ex.getResponseBody()) {
        out.write(bytes);
      }
    }
    ex.close();
  }

  private static void sleep(long millis) {
    if (millis > 0) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }

  /** A non-streaming chat completion with the given assistant text and optional usage. */
  static String completion(String content, Integer promptTokens, Integer completionTokens) {
    String usage =
        promptTokens == null
            ? ""
            : ",\"usage\":{\"prompt_tokens\":"
                + promptTokens
                + ",\"completion_tokens\":"
                + completionTokens
                + ",\"total_tokens\":"
                + (promptTokens + completionTokens)
                + "}";
    return "{\"id\":\"c1\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"vendor/free-model:free\","
        + "\"choices\":[{\"index\":0,\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":"
        + quote(content)
        + "}}]"
        + usage
        + "}";
  }

  static String chunk(String content) {
    return "{\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"vendor/free-model:free\","
        + "\"choices\":[{\"index\":0,\"delta\":{\"content\":"
        + quote(content)
        + "}}]}";
  }

  static String usageChunk(int prompt, int completion) {
    return "{\"id\":\"c1\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"vendor/free-model:free\","
        + "\"choices\":[],\"usage\":{\"prompt_tokens\":"
        + prompt
        + ",\"completion_tokens\":"
        + completion
        + ",\"total_tokens\":"
        + (prompt + completion)
        + "}}";
  }

  private static String quote(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }
}
