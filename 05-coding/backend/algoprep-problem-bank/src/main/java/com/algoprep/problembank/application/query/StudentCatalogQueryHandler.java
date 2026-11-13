package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.StudentCatalogViews.LevelProgressItem;
import com.algoprep.problembank.application.dto.StudentCatalogViews.MyLevelProgress;
import com.algoprep.problembank.application.dto.StudentCatalogViews.TopicProgressItem;
import com.algoprep.problembank.application.dto.StudentCatalogViews.TopicProgressList;
import com.algoprep.problembank.application.ports.in.StudentCatalogUseCase;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import com.algoprep.problembank.domain.model.StudentCatalogModels.LevelProgress;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Topic progress degrades quietly when identity cannot answer (solved is null); level progress
 * answers 503 instead, so a failing read can never show up as "0 solved" (DD F2 14A.3).
 */
@Service
class StudentCatalogQueryHandler implements StudentCatalogUseCase {
  private final StudentCatalogReadRepository repository;
  private final SolveStatePort solveStates;

  StudentCatalogQueryHandler(StudentCatalogReadRepository repository, SolveStatePort solveStates) {
    this.repository = repository;
    this.solveStates = solveStates;
  }

  @Override
  @Transactional(readOnly = true)
  public TopicProgressList topicProgress(UUID userId) {
    var states = solveStates.mine(userId).orElse(null);
    var items =
        repository.topicProgress(states).stream()
            .map(
                t ->
                    new TopicProgressItem(
                        t.topicId(), t.name(), t.sortOrder(), t.solved(), t.total()))
            .toList();
    return new TopicProgressList(items, states != null);
  }

  @Override
  @Transactional(readOnly = true)
  public MyLevelProgress levelProgress(UUID userId) {
    var states =
        solveStates
            .mine(userId)
            .orElseThrow(
                () ->
                    new ServiceUnavailableException("PRB-057", "Solve state is unavailable", null));
    var levels = repository.levelProgress(states);
    return new MyLevelProgress(
        levels.stream().mapToInt(LevelProgress::total).sum(),
        levels.stream().mapToInt(LevelProgress::solved).sum(),
        levels.stream()
            .map(
                l ->
                    new LevelProgressItem(
                        l.code(), l.displayName(), l.sortOrder(), l.total(), l.solved()))
            .toList());
  }

  @Override
  @Transactional(readOnly = true)
  public CatalogSummary summary() {
    return repository.summary();
  }
}
