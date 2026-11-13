package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.ListProblemsQuery;
import com.algoprep.problembank.application.ports.in.ListAuthoringProblemsUseCase;
import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.model.AuthoringListFilter;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Strict validation (400 PRB-001), then one scoped read; an instructor only sees own problems. */
@Service
class ListAuthoringProblemsQueryHandler implements ListAuthoringProblemsUseCase {
  static final int Q_MAX = 100;
  static final Set<Integer> PAGE_SIZES = Set.of(8, 20, 50);
  static final Set<String> SORTS =
      Set.of("code", "title", "level", "status", "submissionCount", "acRate", "updatedAt");

  private final ProblemAuthoringListRepository repository;

  ListAuthoringProblemsQueryHandler(ProblemAuthoringListRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public AuthoringListPage list(ListProblemsQuery query) {
    String q = query.q() == null || query.q().isBlank() ? null : query.q().trim();
    if (q != null && q.length() > Q_MAX) {
      throw new ProblemContentInvalidException("q", "maxLength");
    }
    String status = query.status();
    if (status != null && !status.equals("UNPUBLISHED") && !status.equals("PUBLISHED")) {
      throw new ProblemContentInvalidException("status", "enum");
    }
    String sort = query.sort() == null ? "updatedAt" : query.sort();
    if (!SORTS.contains(sort)) {
      throw new ProblemContentInvalidException("sort", "enum");
    }
    boolean ascending;
    if (query.dir() == null || query.dir().equals("desc")) {
      ascending = false;
    } else if (query.dir().equals("asc")) {
      ascending = true;
    } else {
      throw new ProblemContentInvalidException("dir", "enum");
    }
    int page = query.page() == null ? 1 : query.page();
    int size = query.pageSize() == null ? 8 : query.pageSize();
    if (page < 1) {
      throw new ProblemContentInvalidException("page", "positiveInt");
    }
    if (!PAGE_SIZES.contains(size)) {
      throw new ProblemContentInvalidException("pageSize", "enum");
    }
    return repository.list(
        new AuthoringListFilter(
            q,
            level(query.level()),
            status,
            sort,
            ascending,
            page,
            size,
            query.admin() ? null : query.actorId()));
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
