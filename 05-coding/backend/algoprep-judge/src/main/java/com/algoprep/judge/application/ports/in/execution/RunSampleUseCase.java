package com.algoprep.judge.application.ports.in.execution;

import com.algoprep.judge.application.dto.execution.SampleRun.Command;
import com.algoprep.judge.application.dto.execution.SampleRun.Result;

public interface RunSampleUseCase {
  Result run(Command command);
}
