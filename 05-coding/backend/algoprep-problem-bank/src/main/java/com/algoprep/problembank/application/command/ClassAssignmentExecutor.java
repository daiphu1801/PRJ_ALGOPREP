package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentRepository;
import com.algoprep.problembank.domain.ports.out.ProblemPublishRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * One problem, one transaction: the row and its Outbox event commit or roll back together, and the
 * orchestrator calls this from outside any transaction so a failure costs only that problem.
 */
@Component
class ClassAssignmentExecutor {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final ClassAssignmentRepository assignments;
  private final ProblemPublishRepository outbox;
  private final IdGeneratorPort ids;

  ClassAssignmentExecutor(
      ClassAssignmentRepository assignments, ProblemPublishRepository outbox, IdGeneratorPort ids) {
    this.assignments = assignments;
    this.outbox = outbox;
    this.ids = ids;
  }

  /** True when a new assignment was created, false when the problem was already assigned. */
  @Transactional
  public boolean assign(UUID actor, UUID classId, UUID problemId) {
    var state =
        assignments
            .problemState(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    if (!state.published()) {
      throw new DomainRuleViolationException("PRB-037", "Problem is no longer published");
    }
    if (!assignments.insertActive(problemId, classId, actor)) {
      return false;
    }
    var at = Instant.now();
    var payload =
        JSON.createObjectNode()
            .put("problemId", problemId.toString())
            .put("classId", classId.toString())
            .put("actorUserId", actor.toString())
            .put("problemTitle", state.title())
            .put("assignedAt", at.toString());
    append(problemId, "ProblemAssignedToClass", "problem.assignment.assigned", payload, at);
    return true;
  }

  @Transactional
  public void remove(UUID actor, UUID classId, UUID problemId) {
    var at =
        assignments
            .deactivate(classId, problemId)
            .orElseThrow(
                () ->
                    new ResourceConflictException(
                        "PRB-025", "The problem is no longer assigned to this class"));
    var payload =
        JSON.createObjectNode()
            .put("problemId", problemId.toString())
            .put("classId", classId.toString())
            .put("actorUserId", actor.toString())
            .put("removedAt", at.toString());
    append(problemId, "ProblemUnassignedFromClass", "problem.assignment.removed", payload, at);
  }

  private void append(
      UUID problemId,
      String eventType,
      String routingKey,
      tools.jackson.databind.node.ObjectNode payload,
      Instant at) {
    var envelope =
        JSON.createObjectNode()
            .put("eventId", ids.newId().toString())
            .put("eventType", eventType)
            .put("occurredAt", at.toString())
            .put("schemaVersion", 1)
            .set("payload", payload)
            .toString();
    try {
      outbox.appendEvent(problemId, eventType, routingKey, envelope);
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException(
          "PRB-056", "Problem event persistence unavailable", null);
    }
  }
}
