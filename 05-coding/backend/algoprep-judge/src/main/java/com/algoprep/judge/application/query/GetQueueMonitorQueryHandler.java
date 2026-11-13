package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.QueueJobsQuery;
import com.algoprep.judge.application.ports.in.GetQueueSnapshotUseCase;
import com.algoprep.judge.application.ports.in.ListQueueJobsUseCase;
import com.algoprep.judge.domain.model.QueueMonitoring.Jobs;
import com.algoprep.judge.domain.model.QueueMonitoring.Snapshot;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetQueueMonitorQueryHandler
    implements GetQueueSnapshotUseCase, ListQueueJobsUseCase, InitializingBean {
  private final QueueMonitorRepository repository;
  private final int maximum;

  GetQueueMonitorQueryHandler(
      QueueMonitorRepository repository, @Value("${judge.autoscale.max-workers:8}") int maximum) {
    this.repository = repository;
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
    return Snapshot.from(repository.snapshot(), maximum);
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
