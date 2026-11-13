package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.QueueJobsQuery;
import com.algoprep.judge.application.ports.in.GetQueueSnapshotUseCase;
import com.algoprep.judge.application.ports.in.ListQueueJobsUseCase;
import com.algoprep.judge.domain.model.QueueMonitoring.Jobs;
import com.algoprep.judge.domain.model.QueueMonitoring.Snapshot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge/admin/queue")
class QueueMonitorController {
  private final GetQueueSnapshotUseCase snapshot;
  private final ListQueueJobsUseCase jobs;

  QueueMonitorController(GetQueueSnapshotUseCase snapshot, ListQueueJobsUseCase jobs) {
    this.snapshot = snapshot;
    this.jobs = jobs;
  }

  @GetMapping("/snapshot")
  @PreAuthorize("hasAuthority('JUDGE_QUEUE_MONITOR:READ')")
  Snapshot snapshot() {
    return snapshot.read();
  }

  @GetMapping("/jobs")
  @PreAuthorize("hasAuthority('JUDGE_QUEUE_MONITOR:READ')")
  Jobs jobs(
      @RequestParam(required = false) String tab, @RequestParam(required = false) String page) {
    return jobs.list(QueueJobsQuery.parse(tab, page));
  }
}
