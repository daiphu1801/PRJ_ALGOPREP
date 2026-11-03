package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.SampleRun.Preparation;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;

public interface SampleProgramPort {
  Prepared prepare(Preparation request);
}
