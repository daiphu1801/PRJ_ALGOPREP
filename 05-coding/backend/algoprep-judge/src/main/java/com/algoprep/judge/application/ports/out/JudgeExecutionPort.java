package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;

/** A compilation owns cached files on one engine instance until close, including on failures. */
public interface JudgeExecutionPort {
  CompiledExecution compile(CompileRequest request);

  interface CompiledExecution extends AutoCloseable {
    Compilation compilation();

    RunResult run(RunRequest request);

    @Override
    void close();
  }
}
