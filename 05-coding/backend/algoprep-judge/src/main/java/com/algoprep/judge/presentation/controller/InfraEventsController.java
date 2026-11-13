package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.InfraEventQuery;
import com.algoprep.judge.application.ports.in.GetInfraSummaryUseCase;
import com.algoprep.judge.application.ports.in.ListInfraEventsUseCase;
import com.algoprep.judge.domain.model.InfraEvents.Page;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge/admin")
class InfraEventsController {
  private final GetInfraSummaryUseCase summary;
  private final ListInfraEventsUseCase events;

  InfraEventsController(GetInfraSummaryUseCase summary, ListInfraEventsUseCase events) {
    this.summary = summary;
    this.events = events;
  }

  @GetMapping("/queue/infra-summary")
  @PreAuthorize("hasAuthority('JUDGE_QUEUE_MONITOR:READ')")
  Summary summary() {
    return summary.summary();
  }

  record Filters(String q, String level, String service, String from, String to, String cursor) {}

  @GetMapping("/infra-events")
  @PreAuthorize("hasAuthority('JUDGE_QUEUE_MONITOR:READ')")
  Page events(@ModelAttribute Filters filters) {
    return events.list(
        InfraEventQuery.parse(
            filters.q(),
            filters.level(),
            filters.service(),
            InfraEventQuery.dates(filters.from(), filters.to()),
            filters.cursor()));
  }
}
