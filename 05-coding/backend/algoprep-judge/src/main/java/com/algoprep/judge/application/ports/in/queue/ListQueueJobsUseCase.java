package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.application.dto.queue.QueueJobsQuery;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Jobs;

/** API #14: count-consistent page of waiting/failed/all queue jobs. */
public interface ListQueueJobsUseCase {
  Jobs list(QueueJobsQuery query);
}
