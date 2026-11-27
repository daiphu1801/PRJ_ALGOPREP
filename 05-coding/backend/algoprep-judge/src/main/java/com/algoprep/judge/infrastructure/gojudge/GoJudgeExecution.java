package com.algoprep.judge.infrastructure.gojudge;

import com.algoprep.judge.application.classify.RuntimeClassifier;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort.CompiledExecution;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;

/** A scoped, sequential compile/run lifecycle. Close is mandatory even on compiler failure. */
final class GoJudgeExecution implements CompiledExecution {
  private final GoJudgeTransport transport;
  private final CompileRequest request;
  private final Map<String, String> ids = new LinkedHashMap<>();
  private final Set<String> ownedIds = new LinkedHashSet<>();
  private Compilation compilation;
  private boolean closed;
  private boolean recompiled;

  GoJudgeExecution(GoJudgeTransport transport, CompileRequest request) {
    this.transport = transport;
    this.request = request;
  }

  void compile() {
    ids.clear();
    if (request.program().body().get("compileCmd") == null) {
      if (request.program().artifact() != null) {
        throw new JudgeInfrastructureException();
      }
      compilation = new Compilation(true, "");
      return;
    }
    var command = GoJudgeCommands.compile(request.program(), request.limits());
    var result =
        retry(
            () -> {
              JsonNode row;
              try {
                row = execute(command, request.limits().timeMs(), request.limits().outputBytes());
              } catch (GoJudgeTransport.MissingArtifact missing) {
                throw new JudgeInfrastructureException();
              }
              if (row.path("status").asString().equals("File Error")) {
                throw new JudgeInfrastructureException();
              }
              if (row.path("status").asString().equals("Accepted")
                  && request.program().artifact() != null
                  && !row.path("fileIds").has(request.program().artifact())) {
                throw new JudgeInfrastructureException();
              }
              return row;
            });
    captureFiles(result);
    String status = result.path("status").asString();
    boolean success = status.equals("Accepted") && result.path("exitStatus").asInt() == 0;
    compilation =
        new Compilation(
            success,
            result.path("files").path("stderr").asString("")
                + result.path("files").path("stdout").asString(""));
  }

  @Override
  public Compilation compilation() {
    return compilation;
  }

  @Override
  public RunResult run(RunRequest run) {
    if (closed || !compilation.success()) {
      throw new IllegalStateException("Compilation is not executable");
    }
    var result = retry(() -> executeRun(run));
    String stderr = result.path("files").path("stderr").asString("");
    var program = request.program();
    var status =
        new RuntimeClassifier()
            .classify(
                result.path("status").asString(),
                result.path("exitStatus").asInt(),
                stderr,
                program.model(),
                program.language());
    return new RunResult(
        status,
        result.path("files").path("stdout").asString(""),
        stderr,
        result.path("time").asLong() / 1000000,
        result.path("memory").asLong() / 1024);
  }

  private JsonNode executeRun(RunRequest run) {
    var command = GoJudgeCommands.run(request.program(), ids, run.stdin(), run.limits());
    JsonNode row;
    try {
      row = execute(command, run.limits().timeMs(), run.limits().outputBytes() + 4112);
    } catch (GoJudgeTransport.MissingArtifact missing) {
      return recoverAndRun(run);
    }
    if (row.path("status").asString().equals("File Error")) {
      return recoverAndRun(run);
    }
    return row;
  }

  private JsonNode recoverAndRun(RunRequest run) {
    if (request.program().artifact() == null || recompiled) {
      throw new JudgeInfrastructureException();
    }
    recompiled = true;
    compile();
    if (!compilation.success()) {
      throw new JudgeInfrastructureException();
    }
    return executeRun(run);
  }

  private JsonNode execute(Map<String, Object> command, long timeout, long cap) {
    // JSON escaping can expand one output byte to six; bound before parsing the response tree.
    long responseLimit = Math.min(Integer.MAX_VALUE - 1L, 6 * (cap + 65536) + 65536);
    var result = transport.run(command, timeout + 5000, responseLimit);
    if (!result.isArray() || result.size() != 1) {
      throw new JudgeInfrastructureException();
    }
    var row = result.get(0);
    captureFiles(row);
    String status = row.path("status").asString("");
    if (!java.util.Set.of(
            "Accepted",
            "Nonzero Exit Status",
            "Signalled",
            "Time Limit Exceeded",
            "Memory Limit Exceeded",
            "Output Limit Exceeded",
            "File Error")
        .contains(status)) {
      throw new JudgeInfrastructureException();
    }
    return row;
  }

  private void captureFiles(JsonNode result) {
    result
        .path("fileIds")
        .properties()
        .forEach(
            entry -> {
              String id = entry.getValue().asString();
              ids.put(entry.getKey(), id);
              ownedIds.add(id);
            });
  }

  private <T> T retry(Supplier<T> action) {
    for (int attempt = 0; ; attempt++) {
      try {
        return action.get();
      } catch (JudgeInfrastructureException failure) {
        if (attempt == 2 || Thread.currentThread().isInterrupted()) {
          throw failure;
        }
        try {
          Thread.sleep(attempt == 0 ? 500 : 1000);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new JudgeInfrastructureException();
        }
      }
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    JudgeInfrastructureException failure = null;
    for (String id : ownedIds) {
      try {
        retry(
            () -> {
              transport.delete(id);
              return true;
            });
      } catch (JudgeInfrastructureException cleanup) {
        failure = cleanup;
      }
    }
    ids.clear();
    ownedIds.clear();
    if (failure != null) {
      throw failure;
    }
  }
}
