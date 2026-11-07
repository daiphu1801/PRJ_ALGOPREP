package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.dto.TestcaseMutationResults.Saved;
import com.algoprep.problembank.application.policy.TestcaseViewMapper;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * What the four testcase operations share: the parent lock with the author rule, and closing a
 * change (new grading-set version when it applies, then the {@code updated_at} stamp).
 */
@Component
class TestcaseAuthoringSupport {
  private final ProblemContentRepository content;
  private final TestcaseWriteRepository testcases;
  private final PublishTestcaseSnapshotUseCase snapshots;
  private final ProblemListVersionPort cache;
  private final TestcaseViewMapper views;

  TestcaseAuthoringSupport(
      ProblemContentRepository content,
      TestcaseWriteRepository testcases,
      PublishTestcaseSnapshotUseCase snapshots,
      ProblemListVersionPort cache,
      TestcaseViewMapper views) {
    this.content = content;
    this.testcases = testcases;
    this.snapshots = snapshots;
    this.cache = cache;
    this.views = views;
  }

  /** Locks the problem. Another author's draft is 404 PRB-010, their published one 403 PRB-003. */
  ProblemContentSnapshot lock(AuthoringScope scope, UUID problemId) {
    var problem =
        content
            .lock(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    if (!scope.admin() && !problem.authorId().equals(scope.actorId())) {
      if (!"PUBLISHED".equals(problem.status())) {
        throw new ResourceNotFoundException("PRB-010", "Problem not found");
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    return problem;
  }

  /**
   * Ends a change. On a published problem a change to the approved set publishes a new snapshot
   * (the kernel reuses the current one when the content is logically unchanged).
   */
  ProblemTouch settle(ProblemContentSnapshot problem, UUID actorId, boolean affectsGradingSet) {
    if (affectsGradingSet && "PUBLISHED".equals(problem.status())) {
      snapshots.publishSnapshot(problem.id());
    }
    var touch = testcases.touch(problem.id(), actorId);
    cache.incrementAfterCommit();
    return touch;
  }

  Saved saved(StoredTestcase testcase, ProblemTouch touch) {
    return new Saved(view(testcase), touch.testcaseSetVersion(), touch.updatedAt());
  }

  TestcaseView view(StoredTestcase testcase) {
    return views.view(testcase.authoring());
  }
}
