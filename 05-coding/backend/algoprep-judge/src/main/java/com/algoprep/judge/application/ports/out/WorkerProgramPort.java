package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.dto.WorkerPreparation.Request;

public interface WorkerProgramPort {
  Prepared prepare(Request request);
}
