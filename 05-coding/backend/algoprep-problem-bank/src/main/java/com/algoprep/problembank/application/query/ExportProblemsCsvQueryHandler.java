package com.algoprep.problembank.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.problembank.application.dto.ExportProblemsQuery;
import com.algoprep.problembank.application.dto.ExportedCsv;
import com.algoprep.problembank.application.ports.in.ExportProblemsCsvUseCase;
import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The same scoped query as the list, without paging. Ids outside the caller's scope are dropped
 * without a word (an instructor cannot probe other authors), and nothing is written to the audit.
 */
@Service
class ExportProblemsCsvQueryHandler implements ExportProblemsCsvUseCase {
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);

  private final ProblemAuthoringListRepository repository;
  private final ClockPort clock;

  ExportProblemsCsvQueryHandler(ProblemAuthoringListRepository repository, ClockPort clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public ExportedCsv export(ExportProblemsQuery query) {
    boolean hasIds = query.problemIds() != null;
    boolean hasFilter = query.filter() != null;
    if (hasIds == hasFilter) {
      throw new ProblemContentInvalidException("problemIds", "exactlyOneOfIdsOrFilter");
    }
    var scope = query.scope().authorFilter();
    var items =
        hasIds
            ? repository.export(scope, ids(query.problemIds()), null, null, null)
            : filtered(scope, query.filter());
    String name = "problems-" + DAY.format(clock.now().atOffset(LOCAL)) + ".csv";
    return new ExportedCsv(name, ProblemCsv.render(items).getBytes(StandardCharsets.UTF_8));
  }

  private List<com.algoprep.problembank.domain.model.AuthoringListPage.Item> filtered(
      UUID scope, ExportProblemsQuery.Filter f) {
    String q = f.q() == null || f.q().isBlank() ? null : f.q().trim();
    if (q != null && q.length() > ListAuthoringProblemsQueryHandler.Q_MAX) {
      throw new ProblemContentInvalidException("q", "maxLength");
    }
    String status = f.status();
    if (status != null && !status.equals("UNPUBLISHED") && !status.equals("PUBLISHED")) {
      throw new ProblemContentInvalidException("status", "enum");
    }
    return repository.export(scope, null, q, level(f.level()), status);
  }

  private static List<UUID> ids(List<UUID> raw) {
    if (raw.isEmpty() || raw.stream().anyMatch(java.util.Objects::isNull)) {
      throw new ProblemContentInvalidException("problemIds", "size");
    }
    return List.copyOf(new LinkedHashSet<>(raw));
  }

  private static UUID level(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ProblemContentInvalidException("level", "uuid");
    }
  }
}
