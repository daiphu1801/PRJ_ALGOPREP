package com.algoprep.judge.infrastructure.gojudge;

import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** One fixed engine endpoint per adapter; cached artifacts never migrate between instances. */
@Component
public final class GoJudgeAdapter implements JudgeExecutionPort {
  private final GoJudgeTransport transport;

  public GoJudgeAdapter(@Value("${judge.go-judge.base-url:http://localhost:5050}") String baseUrl) {
    var base = URI.create(baseUrl);
    if (!"http".equals(base.getScheme()) && !"https".equals(base.getScheme())) {
      throw new IllegalArgumentException("Invalid judge endpoint");
    }
    transport = new GoJudgeTransport(base);
  }

  @Override
  public CompiledExecution compile(CompileRequest request) {
    var execution = new GoJudgeExecution(transport, request);
    try {
      execution.compile();
      return execution;
    } catch (RuntimeException failure) {
      try {
        execution.close();
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }
}
