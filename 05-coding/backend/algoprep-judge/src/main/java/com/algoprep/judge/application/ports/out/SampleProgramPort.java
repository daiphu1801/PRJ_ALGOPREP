package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.execution.SampleRun.Preparation;
import com.algoprep.judge.application.dto.execution.WorkerPreparation.Prepared;

public interface SampleProgramPort {
  Prepared prepare(Preparation request);
}
