package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.WorkerControl;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerSettings;
import java.util.Optional;
import java.util.UUID;

public interface WorkerJobRepository {
  Optional<WorkerJob> find(UUID id);

  WorkerSettings settings(String language);

  WorkerControl control();
}
