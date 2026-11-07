package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.BulkProblemActionResult;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.ports.in.BulkProblemActionsUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemPublishStatusUseCase;
import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemBulkRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs a batch outside any transaction and hands each id to a unit that owns its own: one refused
 * or failing problem is reported in {@code failedItems} and never undoes or blocks another. Publish
 * and unpublish reuse the single-problem use case, so the checklist, the sandbox run, the audit and
 * the events are exactly those of section 40.
 */
@Service
class BulkProblemCommandHandler implements BulkProblemActionsUseCase {
  static final int PUBLISH_MAX = 20;
  static final int BATCH_MAX = 200;
  private static final Logger LOG = LoggerFactory.getLogger(BulkProblemCommandHandler.class);

  private final BulkProblemItemExecutor executor;
  private final UpdateProblemPublishStatusUseCase status;
  private final ProblemAuthoringReadRepository reads;
  private final ProblemBulkRepository bulk;

  BulkProblemCommandHandler(
      BulkProblemItemExecutor executor,
      UpdateProblemPublishStatusUseCase status,
      ProblemAuthoringReadRepository reads,
      ProblemBulkRepository bulk) {
    this.executor = executor;
    this.status = status;
    this.reads = reads;
    this.bulk = bulk;
  }

  @Override
  public BulkProblemActionResult publish(AuthoringScope scope, List<UUID> problemIds) {
    return run(problemIds, PUBLISH_MAX, id -> changeStatus(scope, id, "PUBLISHED"));
  }

  @Override
  public BulkProblemActionResult unpublish(AuthoringScope scope, List<UUID> problemIds) {
    return run(problemIds, BATCH_MAX, id -> changeStatus(scope, id, "UNPUBLISHED"));
  }

  @Override
  public BulkProblemActionResult changeDifficulty(
      AuthoringScope scope, List<UUID> problemIds, UUID levelId) {
    if (levelId == null) {
      throw new ProblemContentInvalidException("levelId", "required");
    }
    var ids = ids(problemIds, BATCH_MAX);
    if (!bulk.levelExists(levelId)) {
      throw catalogueMissing();
    }
    return run(ids, id -> executor.changeDifficulty(scope, id, levelId));
  }

  @Override
  public BulkProblemActionResult assignTopic(
      AuthoringScope scope, List<UUID> problemIds, UUID topicId) {
    if (topicId == null) {
      throw new ProblemContentInvalidException("topicId", "required");
    }
    var ids = ids(problemIds, BATCH_MAX);
    if (!bulk.topicExists(topicId)) {
      throw catalogueMissing();
    }
    return run(ids, id -> executor.assignTopic(scope, id, topicId));
  }

  @Override
  public BulkProblemActionResult delete(AuthoringScope scope, List<UUID> problemIds) {
    return run(problemIds, BATCH_MAX, id -> executor.delete(scope, id));
  }

  /**
   * Publish and unpublish carry no timestamp from the screen, so the current one is read just
   * before: a concurrent edit in between shows up as a 409 for that problem only.
   */
  private void changeStatus(AuthoringScope scope, UUID id, String target) {
    var current = reads.find(id, false);
    if (current.isEmpty()) {
      throw new com.algoprep.common.exception.ResourceNotFoundException(
          "PRB-010", "Problem not found");
    }
    status.update(
        new UpdateProblemStatusCommand(
            id, scope.actorId(), scope.admin(), target, current.get().updatedAt()));
  }

  private BulkProblemActionResult run(List<UUID> problemIds, int max, Consumer<UUID> action) {
    return run(ids(problemIds, max), action);
  }

  private BulkProblemActionResult run(List<UUID> ids, Consumer<UUID> action) {
    var succeeded = new ArrayList<UUID>();
    var failed = new ArrayList<BulkProblemActionResult.Failed>();
    for (UUID id : ids) {
      try {
        action.accept(id);
        succeeded.add(id);
      } catch (AlgoPrepException refused) {
        failed.add(
            new BulkProblemActionResult.Failed(
                id,
                refused.errorCode(),
                refused.getMessage(),
                refused instanceof ClientErrorDetails d ? d.clientErrorDetails() : null));
      } catch (RuntimeException unexpected) {
        LOG.warn("Bulk problem action failed for one problem", unexpected);
        failed.add(
            new BulkProblemActionResult.Failed(
                id, "PRB-050", "The action could not be completed", null));
      }
    }
    return new BulkProblemActionResult(succeeded, failed);
  }

  private static List<UUID> ids(List<UUID> raw, int max) {
    if (raw == null
        || raw.isEmpty()
        || raw.size() > max
        || raw.stream().anyMatch(java.util.Objects::isNull)) {
      throw new ProblemContentInvalidException("problemIds", "size");
    }
    return List.copyOf(new LinkedHashSet<>(raw));
  }

  private static DomainRuleViolationException catalogueMissing() {
    return new DomainRuleViolationException("PRB-039", "Selected level or topic no longer exists");
  }
}
