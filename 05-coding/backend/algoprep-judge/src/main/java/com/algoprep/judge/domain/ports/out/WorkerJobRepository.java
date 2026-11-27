package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.execution.WorkerControl;
import com.algoprep.judge.domain.model.execution.WorkerJob;
import com.algoprep.judge.domain.model.execution.WorkerReport;
import com.algoprep.judge.domain.model.execution.WorkerSettings;
import java.util.Optional;
import java.util.UUID;

public interface WorkerJobRepository {
  Optional<WorkerJob> find(UUID id);

  WorkerSettings settings(String language);

  WorkerControl control();

  /** Upserts the single worker status row (judge.worker_status); throws on a database fault. */
  void report(WorkerReport report);
}
