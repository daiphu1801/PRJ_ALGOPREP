package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemBulkRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * One problem, one transaction: the orchestrator calls these from outside any transaction, so a
 * failure rolls back that problem alone (an audit failure included) and the next one still runs.
 * The author rule is the one of the single-problem writes: another author's draft is 404, their
 * published problem is 403 PRB-003, an admin is not bound to authorship.
 */
@Component
class BulkProblemItemExecutor {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final ProblemContentRepository content;
  private final ProblemBulkRepository bulk;
  private final ProblemAuditPort audit;
  private final ProblemListVersionPort cache;
  private final StatusChangeRecorder recorder;

  BulkProblemItemExecutor(
      ProblemContentRepository content,
      ProblemBulkRepository bulk,
      ProblemAuditPort audit,
      ProblemListVersionPort cache,
      StatusChangeRecorder recorder) {
    this.content = content;
    this.bulk = bulk;
    this.audit = audit;
    this.cache = cache;
    this.recorder = recorder;
  }

  @Transactional
  public void changeDifficulty(AuthoringScope scope, UUID problemId, UUID levelId) {
    var old = lockOwned(scope, problemId);
    content.lockCatalogues();
    if (!bulk.levelExists(levelId)) {
      throw catalogueMissing();
    }
    UUID current = old.content().levelId();
    if (levelId.equals(current)) {
      return;
    }
    bulk.setLevel(problemId, scope.actorId(), levelId);
    record(
        scope,
        "PROBLEM_DIFFICULTY_CHANGED",
        problemId,
        json("levelId", current),
        json("levelId", levelId));
    cache.incrementAfterCommit();
  }

  @Transactional
  public void assignTopic(AuthoringScope scope, UUID problemId, UUID topicId) {
    lockOwned(scope, problemId);
    content.lockCatalogues();
    if (!bulk.topicExists(topicId)) {
      throw catalogueMissing();
    }
    if (bulk.addTopic(problemId, scope.actorId(), topicId)) {
      record(scope, "PROBLEM_TOPIC_ASSIGNED", problemId, null, json("topicId", topicId));
      cache.incrementAfterCommit();
    }
  }

  @Transactional
  public void delete(AuthoringScope scope, UUID problemId) {
    var old = lockOwned(scope, problemId);
    var at = bulk.markDeleted(problemId, scope.actorId());
    record(
        scope,
        "PROBLEM_DELETED",
        problemId,
        json("status", old.status()),
        JSON.createObjectNode().put("deleted", true).toString());
    if ("PUBLISHED".equals(old.status())) {
      recorder.withdrawnByDelete(problemId, scope.actorId(), at);
    }
    cache.incrementAfterCommit();
  }

  private ProblemContentSnapshot lockOwned(AuthoringScope scope, UUID problemId) {
    var old =
        content
            .lock(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    if (!scope.admin() && !old.authorId().equals(scope.actorId())) {
      if (!"PUBLISHED".equals(old.status())) {
        throw new ResourceNotFoundException("PRB-010", "Problem not found");
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    return old;
  }

  private void record(
      AuthoringScope scope, String action, UUID problemId, String before, String after) {
    try {
      audit.record(
          new CatalogAuditEntry(
              scope.actorId(), action, "PROBLEM", problemId.toString(), before, after));
    } catch (RuntimeException unavailable) {
      // DD F2 section 57 precedent: an audit failure is 503 PRB-050 and this problem rolls back.
      throw new ServiceUnavailableException("PRB-050", "System audit unavailable", null);
    }
  }

  private static String json(String key, Object value) {
    return JSON.createObjectNode().put(key, String.valueOf(value)).toString();
  }

  private static DomainRuleViolationException catalogueMissing() {
    return new DomainRuleViolationException("PRB-039", "Selected level or topic no longer exists");
  }
}
