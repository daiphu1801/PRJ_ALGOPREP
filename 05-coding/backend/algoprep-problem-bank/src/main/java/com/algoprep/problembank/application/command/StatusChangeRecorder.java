package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemStatusWrite;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemPublishRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** The row write, the audit entry, the Outbox event and the cache bump of one status change. */
@Component
class StatusChangeRecorder {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final ProblemPublishRepository writes;
  private final ProblemAuditPort audit;
  private final ProblemListVersionPort cache;
  private final IdGeneratorPort ids;

  StatusChangeRecorder(
      ProblemPublishRepository writes,
      ProblemAuditPort audit,
      ProblemListVersionPort cache,
      IdGeneratorPort ids) {
    this.writes = writes;
    this.audit = audit;
    this.cache = cache;
    this.ids = ids;
  }

  /** Publishing: {@code ProblemPublished}; withdrawing: {@code ProblemUnpublished}. */
  ProblemStatusWrite change(UpdateProblemStatusCommand command, boolean publish) {
    var write = writes.setStatus(command.problemId(), command.actorId(), command.status());
    String action = publish ? "PROBLEM_PUBLISHED" : "PROBLEM_UNPUBLISHED";
    String event = publish ? "ProblemPublished" : "ProblemUnpublished";
    String key = publish ? "problem.problem.published" : "problem.problem.unpublished";
    try {
      audit.record(
          new CatalogAuditEntry(
              command.actorId(),
              action,
              "PROBLEM",
              command.problemId().toString(),
              null,
              JSON.createObjectNode().put("status", command.status()).toString()));
    } catch (RuntimeException unavailable) {
      // DD F2 section 57 precedent: an audit failure is 503 PRB-050 and the caller rolls back.
      throw new ServiceUnavailableException("PRB-050", "System audit unavailable", null);
    }
    try {
      writes.appendEvent(
          command.problemId(),
          event,
          key,
          envelope(event, command.problemId(), command.actorId(), write.updatedAt()));
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException(
          "PRB-056", "Problem event persistence unavailable", null);
    }
    cache.incrementAfterCommit();
    return write;
  }

  /**
   * Deleting a published problem withdraws it, so listeners get the same {@code
   * ProblemUnpublished}.
   */
  void withdrawnByDelete(UUID problemId, UUID actorId, Instant at) {
    try {
      writes.appendEvent(
          problemId,
          "ProblemUnpublished",
          "problem.problem.unpublished",
          envelope("ProblemUnpublished", problemId, actorId, at));
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException(
          "PRB-056", "Problem event persistence unavailable", null);
    }
  }

  private String envelope(String eventType, UUID problemId, UUID actorId, Instant occurredAt) {
    var payload =
        JSON.createObjectNode()
            .put("problemId", problemId.toString())
            .put("actorUserId", actorId.toString())
            .put("occurredAt", occurredAt.toString());
    return JSON.createObjectNode()
        .put("eventId", ids.newId().toString())
        .put("eventType", eventType)
        .put("occurredAt", occurredAt.toString())
        .put("schemaVersion", 1)
        .set("payload", payload)
        .toString();
  }
}
