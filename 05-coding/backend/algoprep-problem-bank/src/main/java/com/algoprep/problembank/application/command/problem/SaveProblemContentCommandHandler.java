package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.exception.ProblemVersionConflictException;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal ordinary-draft/create and full-state content replacement; no fake cross-module adapters.
 */
@Service
class SaveProblemContentCommandHandler implements SaveProblemContentUseCase {
  private final ProblemContentRepository repository;
  private final ProblemContentValidation validation;
  private final ProblemListVersionPort cache;

  SaveProblemContentCommandHandler(
      ProblemContentRepository repository,
      ProblemContentValidation validation,
      ProblemListVersionPort cache) {
    this.repository = repository;
    this.validation = validation;
    this.cache = cache;
  }

  @Override
  @Transactional
  public ProblemContentSnapshot save(SaveProblemContentCommand command) {
    if (!command.actionGranted()) {
      throw new PermissionDeniedException("PRB-002", "Problem authoring permission required");
    }
    if (command.actorId() == null) {
      throw ProblemContentValidation.invalid("actor", "required");
    }
    var old = command.problemId() == null ? null : authorize(command);
    var data = validation.normalize(command.content());
    validateExamples(old, data);
    repository.lockCatalogues();
    var level = data.levelId();
    if (level == null && old == null) {
      level =
          repository.firstLevel().orElseThrow(SaveProblemContentCommandHandler::catalogueMissing);
    }
    if (level == null) {
      throw ProblemContentValidation.invalid("levelId", "required");
    }
    if (!repository.cataloguesExist(level, data.topicIds())) {
      throw catalogueMissing();
    }
    data =
        new ProblemContentData(
            data.title(),
            data.statementMd(),
            data.constraintsMd(),
            level,
            data.limits(),
            data.topicIds(),
            data.tags(),
            data.workedExamples(),
            data.referenceSolution());
    var id = old == null ? repository.create(command.actorId(), data) : old.id();
    var result = repository.write(id, command.actorId(), data);
    if (old != null && "PUBLISHED".equals(old.status())) {
      cache.incrementAfterCommit();
    }
    return result;
  }

  private ProblemContentSnapshot authorize(SaveProblemContentCommand command) {
    var old =
        repository.lock(command.problemId()).orElseThrow(SaveProblemContentCommandHandler::missing);
    if (!command.admin() && !old.authorId().equals(command.actorId())) {
      if (!"PUBLISHED".equals(old.status())) {
        throw missing();
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    if (command.expectedUpdatedAt() == null) {
      throw ProblemContentValidation.invalid("expectedUpdatedAt", "required");
    }
    if (!old.updatedAt().equals(command.expectedUpdatedAt())) {
      throw new ProblemVersionConflictException(old.updatedAt());
    }
    return old;
  }

  private void validateExamples(ProblemContentSnapshot old, ProblemContentData data) {
    if (old != null && "PUBLISHED".equals(old.status()) && data.workedExamples().size() < 2) {
      throw new DomainRuleViolationException(
          "PRB-053", "Published problem requires two worked examples");
    }
    var owned = new HashSet<java.util.UUID>();
    if (old != null) {
      old.content().workedExamples().forEach(e -> owned.add(e.id()));
    }
    for (var example : data.workedExamples()) {
      if (example.id() != null && !owned.contains(example.id())) {
        throw ProblemContentValidation.invalid("workedExamples.id", "exists");
      }
    }
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }

  private static DomainRuleViolationException catalogueMissing() {
    return new DomainRuleViolationException("PRB-039", "Selected level or topic no longer exists");
  }
}
