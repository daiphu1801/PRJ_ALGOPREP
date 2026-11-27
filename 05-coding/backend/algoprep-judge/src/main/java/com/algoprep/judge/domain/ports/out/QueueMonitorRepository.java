package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.queue.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Facts;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Job;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.JobSettings;
import java.util.List;

/** Read-only own-schema projections; missing settings or SQL faults must fail closed. */
public interface QueueMonitorRepository {
  Facts snapshot();

  JobSettings jobSettings();

  Counts counts(JobSettings settings);

  List<Job> jobs(String tab, JobSettings settings, long offset);
}
