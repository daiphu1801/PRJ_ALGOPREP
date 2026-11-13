package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueueJobsQuery;
import com.algoprep.judge.domain.model.QueueMonitoring.Jobs;

/** API #14: count-consistent page of waiting/failed/all queue jobs. */
public interface ListQueueJobsUseCase {
  Jobs list(QueueJobsQuery query);
}
