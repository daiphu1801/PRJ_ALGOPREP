package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.InfraEventMessages;
import com.algoprep.judge.application.dto.InfraEventQuery;
import com.algoprep.judge.application.ports.in.GetInfraSummaryUseCase;
import com.algoprep.judge.application.ports.in.ListInfraEventsUseCase;
import com.algoprep.judge.domain.model.InfraEvents.Filter;
import com.algoprep.judge.domain.model.InfraEvents.Page;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import com.algoprep.judge.domain.ports.out.InfraEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetInfraEventsQueryHandler implements GetInfraSummaryUseCase, ListInfraEventsUseCase {
  private final InfraEventRepository repository;

  GetInfraEventsQueryHandler(InfraEventRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Summary summary() {
    return repository.summary();
  }

  @Override
  @Transactional(readOnly = true)
  public Page list(InfraEventQuery query) {
    var filter =
        new Filter(
            query.q(),
            query.level(),
            query.service(),
            query.dates().from(),
            query.dates().until(),
            query.cursor());
    var rows = repository.list(filter, InfraEventMessages.templates());
    var visible = rows.subList(0, Math.min(20, rows.size()));
    String cursor = rows.size() > 20 ? InfraEventQuery.encode(visible.getLast().position()) : null;
    return new Page(
        visible.stream().map(InfraEventMessages::event).toList(),
        cursor,
        List.of("go-judge", "outbox"));
  }
}
