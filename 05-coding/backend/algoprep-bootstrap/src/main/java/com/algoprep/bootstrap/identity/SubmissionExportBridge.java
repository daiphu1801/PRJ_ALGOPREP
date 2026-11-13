package com.algoprep.bootstrap.identity;

import com.algoprep.identity.application.dto.SubmissionExport;
import com.algoprep.identity.application.ports.out.SubmissionExportPort;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.ports.in.ListMySubmissionHistoryUseCase;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation plus a paging loop: identity asks for every submission of one user, the judge
 * answers one page of its history at a time (F4 history port, owner decision
 * DEC-2026-1107-f1-backend-clear-order, so F4 has no export port). The whole loop runs on one
 * virtual thread with a single deadline and the request security context; a failure or a missed
 * deadline is one {@link IllegalStateException}, never a partial file.
 */
@Component("submissionExportBridge")
public final class SubmissionExportBridge implements SubmissionExportPort {
  private final ListMySubmissionHistoryUseCase history;
  private final Duration limit;

  @Autowired
  public SubmissionExportBridge(ListMySubmissionHistoryUseCase history) {
    this(history, Duration.ofSeconds(20));
  }

  SubmissionExportBridge(ListMySubmissionHistoryUseCase history, Duration limit) {
    this.history = history;
    this.limit = limit;
  }

  @Override
  public SubmissionExport readAll(UUID userId, int maxRows) {
    return BridgeDeadline.call("submission-export-bridge", limit, () -> loop(userId, maxRows));
  }

  private SubmissionExport loop(UUID userId, int maxRows) {
    // Keyed by id: a submission that arrives between two pages shifts the next page by one row and
    // would otherwise appear twice.
    Map<UUID, SubmissionExport.Row> rows = new LinkedHashMap<>();
    long total = 0;
    for (int page = 1; rows.size() < maxRows; page++) {
      var result =
          history.history(userId, new Filters(null, null, null, String.valueOf(page), null));
      total = result.totalCount();
      for (History h : result.items()) {
        if (rows.size() < maxRows) {
          rows.putIfAbsent(h.submissionId(), row(h));
        }
      }
      if (result.items().isEmpty() || (long) page * result.pageSize() >= total) {
        break;
      }
    }
    return new SubmissionExport(rows.values().stream().toList(), total > rows.size());
  }

  private static SubmissionExport.Row row(History h) {
    var problem = h.problem();
    return new SubmissionExport.Row(
        h.submissionId(),
        h.submittedAt(),
        problem == null ? null : problem.code(),
        problem == null ? null : problem.title(),
        h.language(),
        h.status(),
        h.passedCount(),
        h.totalCount(),
        h.runtimeMs());
  }
}
