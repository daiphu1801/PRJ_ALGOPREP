package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;

record WorkerExecutionRequest(
    WorkerJob job,
    WorkerSettings settings,
    Prepared prepared,
    Limits limits,
    GradingTestcaseSnapshot snapshot,
    Lease lease) {
  @Override
  public String toString() {
    return "WorkerExecutionRequest[id=" + job.id() + "]";
  }
}
