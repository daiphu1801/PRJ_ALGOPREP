package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.SampleRun.Command;
import com.algoprep.judge.application.dto.SampleRun.Result;

public interface RunSampleUseCase {
  Result run(Command command);
}
