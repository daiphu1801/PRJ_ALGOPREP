package com.algoprep.judge.application.dto;

import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.domain.model.WorkerJob;

public final class WorkerPreparation {
  private WorkerPreparation() {}

  public record Request(WorkerJob job, int specVersion) {
    @Override
    public String toString() {
      return "Request[id=" + job.id() + "]";
    }
  }

  public record Prepared(Program program, Matching matching) {
    @Override
    public String toString() {
      return "Prepared[redacted]";
    }
  }
}
