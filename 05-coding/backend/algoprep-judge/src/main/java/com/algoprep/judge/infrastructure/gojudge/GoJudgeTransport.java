package com.algoprep.judge.infrastructure.gojudge;

import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Bounded HTTP transport. Never includes request/response data in exceptions or logs. */
final class GoJudgeTransport {
  private final URI base;
  private final HttpClient client;
  private final JsonMapper json = JsonMapper.builder().build();

  GoJudgeTransport(URI base) {
    this.base = base;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  JsonNode run(Map<String, Object> command, long timeoutMs, long responseLimit) {
    var body = json.writeValueAsString(Map.of("cmd", java.util.List.of(command)));
    var request =
        HttpRequest.newBuilder(base.resolve("/run"))
            .timeout(Duration.ofMillis(timeoutMs))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    return exchange(request, responseLimit);
  }

  void delete(String id) {
    if (!id.matches("[A-Za-z0-9_-]+")) {
      throw new JudgeInfrastructureException();
    }
    var request =
        HttpRequest.newBuilder(base.resolve("/file/" + id))
            .timeout(Duration.ofSeconds(5))
            .DELETE()
            .build();
    exchange(request, 65536);
  }

  private JsonNode exchange(HttpRequest request, long limit) {
    var pending = client.sendAsync(request, info -> new BoundedJudgeBody(limit));
    try {
      var response =
          pending.get(
              request.timeout().orElse(Duration.ofSeconds(5)).toMillis(), TimeUnit.MILLISECONDS);
      if (request.method().equals("DELETE") && response.statusCode() == 404) {
        return json.nullNode();
      }
      var bytes = response.body();
      if (response.statusCode() == 500
          && bytes.length <= 512
          && json.readTree(bytes).asString("").startsWith("file does not exists with id: \"")) {
        throw new MissingArtifact();
      }
      if (bytes.length > limit || response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new JudgeInfrastructureException();
      }
      return bytes.length == 0 ? json.nullNode() : json.readTree(bytes);
    } catch (InterruptedException interrupted) {
      pending.cancel(true);
      Thread.currentThread().interrupt();
      throw new JudgeInfrastructureException();
    } catch (MissingArtifact missing) {
      throw missing;
    } catch (ExecutionException | TimeoutException | RuntimeException failure) {
      pending.cancel(true);
      throw new JudgeInfrastructureException();
    }
  }

  static final class MissingArtifact extends RuntimeException {
    private static final long serialVersionUID = 1L;

    MissingArtifact() {
      super("Cached judge artifact is unavailable");
    }
  }
}
