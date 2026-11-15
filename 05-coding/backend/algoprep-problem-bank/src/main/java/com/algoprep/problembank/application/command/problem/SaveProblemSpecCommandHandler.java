package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.dto.SaveProblemSpecResult;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import com.algoprep.problembank.application.ports.out.ProblemSpecCachePort;
import com.algoprep.problembank.domain.exception.ProblemSpecInvalidException;
import com.algoprep.problembank.domain.exception.ProblemVersionConflictException;
import com.algoprep.problembank.domain.model.ProblemWriteState;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Parent row serializes spec/content writes; durable event and version commit atomically. */
@Service
class SaveProblemSpecCommandHandler implements SaveProblemSpecUseCase {
  private final ProblemSpecRepository repository;
  private final ProblemSpecCachePort cache;
  private final IdGeneratorPort ids;

  SaveProblemSpecCommandHandler(
      ProblemSpecRepository repository, ProblemSpecCachePort cache, IdGeneratorPort ids) {
    this.repository = repository;
    this.cache = cache;
    this.ids = ids;
  }

  @Override
  @Transactional
  public SaveProblemSpecResult save(SaveProblemSpecCommand command) {
    var state =
        repository.lock(command.problemId()).orElseThrow(SaveProblemSpecCommandHandler::notFound);
    authorize(command, state);
    var old = repository.find(command.problemId()).orElse(null);
    var raw = ProblemSpecJson.data(command.body(), old == null ? null : old.data());
    var evaluation = FunctionSpecEvaluator.evaluate(ProblemSpecJson.draft(raw));
    if (!evaluation.violations().isEmpty()) {
      throw new ProblemSpecInvalidException(evaluation.violations());
    }
    ProblemSpecJson.checkEpsilon(raw.epsilonValue());
    var data = ProblemSpecJson.normalize(raw);
    var changed = ProblemSpecJson.changed(old == null ? null : old.data(), data);
    int version = old == null ? 1 : old.specVersion();
    Instant updated = state.updatedAt();
    if (old == null || !changed.isEmpty()) {
      version = old == null ? 1 : Math.incrementExact(version);
      repository.save(command.problemId(), new StoredProblemSpec(data, version));
      updated =
          repository.touch(command.problemId(), command.actorId(), evaluation.wrapperSupported());
      appendEvent(command.problemId(), event(command.problemId(), version, changed, updated));
      cache.invalidateAfterCommit(command.problemId());
    }
    var document = ProblemSpecJson.document(data).put("specVersion", version);
    var warnings =
        evaluation.unsupportedReasons().stream()
            .map(
                r ->
                    new SaveProblemSpecResult.Warning(
                        "PRB-W01",
                        r.code(),
                        r.path(),
                        "Function wrapper unavailable for this specification"))
            .toList();
    return new SaveProblemSpecResult(document, evaluation.wrapperSupported(), warnings, updated);
  }

  private void appendEvent(UUID problemId, String envelope) {
    try {
      repository.appendEvent(problemId, envelope);
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException(
          "PRB-056", "Problem event persistence unavailable", null);
    }
  }

  private void authorize(SaveProblemSpecCommand command, ProblemWriteState state) {
    if (!command.admin() && !state.authorId().equals(command.actorId())) {
      if (!"PUBLISHED".equals(state.status())) {
        throw notFound();
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    if (command.expectedUpdatedAt() == null) {
      throw new InvalidRequestException("PRB-001", "Missing expectedUpdatedAt");
    }
    if (!state.updatedAt().equals(command.expectedUpdatedAt())) {
      throw new ProblemVersionConflictException(state.updatedAt());
    }
  }

  private String event(UUID problemId, int version, List<String> changed, Instant occurredAt) {
    var json = JsonMapper.builder().build();
    var payload =
        json.createObjectNode().put("problemId", problemId.toString()).put("specVersion", version);
    payload.set("changedColumns", json.valueToTree(changed));
    Set<String> signature = Set.of("functionName", "returnType", "parameters", "nameOverrides");
    payload.put("signatureChanged", changed.stream().anyMatch(signature::contains));
    return json.createObjectNode()
        .put("eventId", ids.newId().toString())
        .put("eventType", "ProblemSpecChanged")
        .put("occurredAt", occurredAt.toString())
        .put("schemaVersion", 1)
        .set("payload", payload)
        .toString();
  }

  private static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}
