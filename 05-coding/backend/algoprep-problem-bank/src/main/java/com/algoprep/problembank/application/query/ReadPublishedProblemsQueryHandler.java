package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.StudentProblemViews.Item;
import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.application.dto.StudentProblemViews.ProblemList;
import com.algoprep.problembank.application.dto.StudentProblemViews.RandomProblem;
import com.algoprep.problembank.application.dto.StudentProblemViews.SampleItem;
import com.algoprep.problembank.application.dto.StudentProblemViews.SampleList;
import com.algoprep.problembank.application.ports.in.ReadPublishedProblemsUseCase;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Sample;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Workspace;
import com.algoprep.problembank.domain.ports.out.PublishedProblemReadRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lenient parameter reading (DD F2 1.2): a value that is not valid is dropped and the default
 * applies, so these reads never answer 400. A problem that is not published is 404 PRB-010, the
 * same answer as a missing id.
 */
@Service
class ReadPublishedProblemsQueryHandler implements ReadPublishedProblemsUseCase {
  static final int Q_MAX = 100;
  static final Set<Integer> PAGE_SIZES = Set.of(12, 24, 48);
  static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);
  private static final Set<String> STATUSES = Set.of("SOLVED", "ATTEMPTED", "UNSOLVED");

  private final PublishedProblemReadRepository repository;
  private final SolveStatePort solveStates;
  private final TestcaseObjectStorePort store;

  ReadPublishedProblemsQueryHandler(
      PublishedProblemReadRepository repository,
      SolveStatePort solveStates,
      TestcaseObjectStorePort store) {
    this.repository = repository;
    this.solveStates = solveStates;
    this.store = store;
  }

  @Override
  @Transactional(readOnly = true)
  public ProblemList list(UUID userId, Map<String, String> params) {
    var states = solveStates.mine(userId).orElse(null);
    var paging = paging(params);
    var page = repository.list(filter(params, states != null, false), states, paging);
    int totalPages = Math.max(1, (int) Math.ceil(page.totalItems() / (double) paging.pageSize()));
    var items = new ArrayList<Item>();
    for (var row : page.items()) {
      items.add(
          new Item(
              row.id(),
              row.code(),
              row.title(),
              row.topicNames(),
              row.levelCode(),
              row.levelDisplayName(),
              row.acRate(),
              row.functionWrapperSupported(),
              states == null ? null : stateOf(states, row.id())));
    }
    return new ProblemList(
        items,
        new PageMeta(page.page(), paging.pageSize(), page.totalItems(), totalPages),
        states != null);
  }

  @Override
  @Transactional(readOnly = true)
  public RandomProblem random(UUID userId, Map<String, String> params) {
    var states = solveStates.mine(userId).orElse(null);
    return new RandomProblem(
        repository.random(filter(params, states != null, true), states).orElse(null));
  }

  @Override
  @Transactional(readOnly = true)
  public Workspace workspace(UUID userId, UUID problemId) {
    return repository
        .workspace(problemId, userId)
        .orElseThrow(ReadPublishedProblemsQueryHandler::missing);
  }

  @Override
  @Transactional(readOnly = true)
  public Overview overview(UUID userId, UUID problemId) {
    return repository
        .overview(problemId, userId)
        .orElseThrow(ReadPublishedProblemsQueryHandler::missing);
  }

  @Override
  @Transactional(readOnly = true)
  public SampleList samples(UUID problemId) {
    var found =
        repository.samples(problemId).orElseThrow(ReadPublishedProblemsQueryHandler::missing);
    var items = new ArrayList<SampleItem>();
    for (Sample s : found.items()) {
      boolean inInline = "INLINE".equals(s.input().storage());
      boolean outInline = "INLINE".equals(s.expectedOutput().storage());
      items.add(
          new SampleItem(
              s.testcaseId(),
              s.position(),
              inInline ? s.input().inline() : null,
              inInline ? null : store.presignGet(s.input().objectKey(), DOWNLOAD_TTL),
              outInline ? s.expectedOutput().inline() : null,
              outInline ? null : store.presignGet(s.expectedOutput().objectKey(), DOWNLOAD_TTL)));
    }
    return new SampleList(found.parameterNames(), items);
  }

  private static Filter filter(Map<String, String> params, boolean statesKnown, boolean random) {
    String status = params.get("status");
    boolean valid =
        status != null && (STATUSES.contains(status) || random && "NOT_SOLVED".equals(status));
    return new Filter(
        text(params.get("q")),
        valid && statesKnown ? status : null,
        text(params.get("difficulty")),
        uuid(params.get("topic")),
        text(params.get("tag")));
  }

  private static Paging paging(Map<String, String> params) {
    String sort = params.get("sort");
    if (!"level".equals(sort) && !"acRate".equals(sort)) {
      sort = "code";
    }
    int page = integer(params.get("page"), 1);
    int size = integer(params.get("pageSize"), 12);
    return new Paging(
        sort,
        !"desc".equals(params.get("dir")),
        Math.max(page, 1),
        PAGE_SIZES.contains(size) ? size : 12);
  }

  private static String stateOf(SolveStates states, UUID id) {
    if (states.solved().contains(id)) {
      return "SOLVED";
    }
    return states.attempted().contains(id) ? "ATTEMPTED" : "UNSOLVED";
  }

  private static String text(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.length() > Q_MAX ? trimmed.substring(0, Q_MAX) : trimmed;
  }

  private static UUID uuid(String raw) {
    try {
      return raw == null ? null : UUID.fromString(raw.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static int integer(String raw, int fallback) {
    try {
      return raw == null ? fallback : Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}
