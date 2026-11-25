package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.queue.QueueJobsQuery;
import com.algoprep.judge.application.ports.in.queue.GetQueueSnapshotUseCase;
import com.algoprep.judge.application.ports.in.queue.ListQueueJobsUseCase;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Jobs;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Snapshot;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetQueueMonitorQueryHandler
    implements GetQueueSnapshotUseCase, ListQueueJobsUseCase, InitializingBean {
  private final QueueMonitorRepository repository;
  private final SubmissionReadRepository submissions;
  private final int maximum;

  GetQueueMonitorQueryHandler(
      QueueMonitorRepository repository,
      SubmissionReadRepository submissions,
      @Value("${judge.autoscale.max-workers:8}") int maximum) {
    this.repository = repository;
    this.submissions = submissions;
    this.maximum = maximum;
  }

  @Override
  public void afterPropertiesSet() {
    if (maximum < 1) {
      throw new IllegalStateException("Queue monitor requires a positive worker limit");
    }
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Snapshot read() {
    var facts = repository.snapshot();
    // Same throughput and formula as the learner's queue estimate
    // (DEC-2026-1115-f4-queue-wait-in-progress-snapshot).
    long finished = submissions.finishedLastMinute();
    return Snapshot.from(
        facts,
        maximum,
        finished,
        SubmissionReadProjection.estimate(facts.pendingCount(), finished));
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Jobs list(QueueJobsQuery query) {
    var settings = repository.jobSettings();
    var counts = repository.counts(settings);
    long total = counts.forTab(query.tab());
    int page = query.resolvePage(total, settings.pageSize());
    long offset = (long) (page - 1) * settings.pageSize();
    var items = repository.jobs(query.tab(), settings, offset);
    return new Jobs(items, page, settings.pageSize(), total, counts);
  }
}
