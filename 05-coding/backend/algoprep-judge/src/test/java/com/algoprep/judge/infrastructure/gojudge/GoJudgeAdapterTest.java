package com.algoprep.judge.infrastructure.gojudge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.Language;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Transport fault injection only. Real sandbox evidence is ExecutionBridgeFlowTest. */
class GoJudgeAdapterTest {
  private HttpServer server;
  private GoJudgeAdapter adapter;
  private final JsonMapper json = JsonMapper.builder().build();
  private final List<JsonNode> requests = new ArrayList<>();
  private final List<String> deleted = new ArrayList<>();
  private final AtomicInteger calls = new AtomicInteger();
  private java.util.function.IntFunction<String> response;
  private final Limits limits = new Limits(1000, 268435456, 17, 60);

  @BeforeEach
  void startTransportFixture() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/run",
        exchange -> {
          requests.add(json.readTree(exchange.getRequestBody().readAllBytes()));
          var bytes = response.apply(calls.getAndIncrement()).getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (var body = exchange.getResponseBody()) {
            body.write(bytes);
          }
        });
    server.createContext(
        "/file/",
        exchange -> {
          deleted.add(exchange.getRequestURI().getPath());
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    adapter = new GoJudgeAdapter("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void stopTransportFixture() {
    server.stop(0);
  }

  private CompileRequest compileRequest(String artifact) {
    var template =
        Map.<String, Object>of(
            "args",
            List.of("fixed"),
            "env",
            List.of("LC_ALL=C", "APH_MAX_RESULT_BYTES=untrusted"),
            "copyIn",
            Map.of());
    var program =
        new Program(
            Language.PYTHON,
            Model.FUNCTION_WRAPPER,
            Map.of("compileCmd", template, "runCmd", template),
            artifact,
            null,
            "echo(value)",
            "@@APH-RESULT@@");
    return new CompileRequest(program, limits);
  }

  private String accepted(String id) {
    return "[{\"status\":\"Accepted\",\"exitStatus\":0,\"time\":2000000,\"memory\":4096,"
        + "\"files\":{\"stdout\":\"ok\\n\"},\"fileIds\":{\"main\":\""
        + id
        + "\"}}]";
  }

  @Test
  void compilesOnceAndRunsTwiceWithDataLimitsAndCleanup() {
    response =
        index ->
            index == 0
                ? accepted("cache1")
                : "[{\"status\":\"Accepted\",\"exitStatus\":0,\"files\":{\"stdout\":\"ok\\n\"}}]";
    try (var execution = adapter.compile(compileRequest("main"))) {
      assertThat(execution.compilation().success()).isTrue();
      assertThat(execution.run(new RunRequest("stdin secret", limits)).status())
          .isEqualTo(Status.OK);
      execution.run(new RunRequest("other stdin", limits));
    }
    assertThat(requests).hasSize(3);
    assertThat(requests.getFirst().path("cmd").get(0).path("copyOutCached").get(0).asString())
        .isEqualTo("main");
    var run = requests.get(1).path("cmd").get(0);
    assertThat(run.path("copyIn").path("main").path("fileId").asString()).isEqualTo("cache1");
    assertThat(run.path("files").get(0).path("content").asString()).isEqualTo("stdin secret");
    assertThat(run.path("args").toString()).doesNotContain("secret");
    assertThat(run.path("env").toString())
        .contains("APH_MAX_RESULT_BYTES=17")
        .doesNotContain("untrusted");
    assertThat(run.path("cpuLimit").asLong()).isEqualTo(1000000000);
    assertThat(run.path("memoryLimit").asLong()).isEqualTo(268435456);
    assertThat(deleted).containsExactly("/file/cache1");
  }

  @Test
  void missingCachedFileTriggersOneRecompileAndCleansBothIdentifiers() {
    response =
        index ->
            switch (index) {
              case 0 -> accepted("cache1");
              case 1 -> "[{\"status\":\"File Error\"}]";
              case 2 -> accepted("cache2");
              default ->
                  "[{\"status\":\"Accepted\",\"exitStatus\":0,\"files\":{\"stdout\":\"ok\"}}]";
            };
    try (var execution = adapter.compile(compileRequest("main"))) {
      assertThat(execution.run(new RunRequest("", limits)).status()).isEqualTo(Status.OK);
    }
    assertThat(calls.get()).isEqualTo(4);
    assertThat(deleted).containsExactly("/file/cache1", "/file/cache2");
  }

  @Test
  void compilerFailureHasNoRunAndCleansAnyReturnedFile() {
    response =
        index ->
            "[{\"status\":\"Nonzero Exit Status\",\"exitStatus\":1,\"files\":{\"stderr\":\"compiler data\"},\"fileIds\":{\"main\":\"cache1\"}}]";
    try (var execution = adapter.compile(compileRequest("main"))) {
      assertThat(execution.compilation().success()).isFalse();
      assertThat(execution.compilation().compilerOutput()).isEqualTo("compiler data");
      assertThatThrownBy(() -> execution.run(new RunRequest("", limits)))
          .isInstanceOf(IllegalStateException.class);
    }
    assertThat(calls.get()).isEqualTo(1);
    assertThat(deleted).containsExactly("/file/cache1");
  }

  @Test
  void internalStatusWinsOverMarkersRetriesAndCleansAllFiles() {
    response =
        index ->
            "[{\"status\":\"Internal Error\",\"exitStatus\":92,\"files\":{\"stderr\":\"@@APH-HARNESS-SIGNATURE-ERROR@@\\nSECRET\"},\"fileIds\":{\"main\":\"cache"
                + index
                + "\"}}]";
    assertThatThrownBy(() -> adapter.compile(compileRequest("main")))
        .isInstanceOf(JudgeInfrastructureException.class)
        .hasMessage("Judge sandbox is unavailable");
    assertThat(calls.get()).isEqualTo(3);
    assertThat(deleted).containsExactly("/file/cache0", "/file/cache1", "/file/cache2");
  }

  @Test
  void malformedResponseCannotExposeResponseText() {
    response = index -> "SECRET malformed JSON";
    assertThatThrownBy(() -> adapter.compile(compileRequest(null)))
        .isInstanceOf(JudgeInfrastructureException.class)
        .hasMessage("Judge sandbox is unavailable");
    assertThat(calls.get()).isEqualTo(3);
    assertThatThrownBy(() -> new GoJudgeAdapter("file:///secret"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptedCompileWithoutRequiredArtifactIsRetriedAsInfrastructureFailure() {
    response = index -> "[{\"status\":\"Accepted\",\"exitStatus\":0,\"files\":{}}]";
    assertThatThrownBy(() -> adapter.compile(compileRequest("main")))
        .isInstanceOf(JudgeInfrastructureException.class);
    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  void nullableCompileTemplateSkipsCompileRequestAndRunsSource() {
    response =
        index -> "[{\"status\":\"Accepted\",\"exitStatus\":0,\"files\":{\"stdout\":\"ok\"}}]";
    var original = compileRequest(null).program();
    var body = new java.util.LinkedHashMap<>(original.body());
    body.put("compileCmd", null);
    var program =
        new Program(
            Language.PYTHON,
            Model.FUNCTION_WRAPPER,
            body,
            null,
            null,
            "echo(value)",
            "@@APH-RESULT@@");
    try (var execution = adapter.compile(new CompileRequest(program, limits))) {
      assertThat(execution.compilation().success()).isTrue();
      assertThat(calls.get()).isZero();
      assertThat(execution.run(new RunRequest("", limits)).status()).isEqualTo(Status.OK);
    }
    assertThat(calls.get()).isEqualTo(1);
    assertThat(deleted).isEmpty();
  }

  @Test
  void oversizedHttpBodyIsCancelledBeforeJsonParsing() {
    response = index -> "SECRET".repeat(1000);
    var transport =
        new GoJudgeTransport(
            java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    assertThatThrownBy(() -> transport.run(Map.of(), 1000, 32))
        .isInstanceOf(JudgeInfrastructureException.class)
        .hasMessage("Judge sandbox is unavailable");
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void deadlineAlsoCoversAStalledResponseBody() {
    var release = new java.util.concurrent.CountDownLatch(1);
    var firstChunk = new java.util.concurrent.CountDownLatch(1);
    server.removeContext("/run");
    server.createContext(
        "/run",
        exchange -> {
          exchange.sendResponseHeaders(200, 0);
          try (var body = exchange.getResponseBody()) {
            body.write('[');
            body.flush();
            firstChunk.countDown();
            try {
              release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
            }
          }
        });
    var transport =
        new GoJudgeTransport(
            java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    try {
      assertThatThrownBy(() -> transport.run(Map.of(), 500, 1024))
          .isInstanceOf(JudgeInfrastructureException.class)
          .hasMessage("Judge sandbox is unavailable");
      assertThat(firstChunk.getCount()).isZero();
    } finally {
      release.countDown();
    }
  }
}
