package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * BR-10 (DD F2 section 36.5): what the server copies when a new problem names a source. Content,
 * examples and the reference solution come from the request body; only the specification and the
 * testcases (with their MinIO objects) are copied here, inside the caller's transaction.
 */
@Component
class ProblemDuplicator {
  private final ProblemAuthoringReadRepository reads;
  private final ProblemContentRepository content;
  private final ProblemSpecRepository specs;
  private final TestcaseWriteRepository testcases;
  private final TestcaseContentStore store;

  ProblemDuplicator(
      ProblemAuthoringReadRepository reads,
      ProblemContentRepository content,
      ProblemSpecRepository specs,
      TestcaseWriteRepository testcases,
      TestcaseContentStore store) {
    this.reads = reads;
    this.content = content;
    this.specs = specs;
    this.testcases = testcases;
    this.store = store;
  }

  /**
   * The source with testcase references, checked before anything is written. Missing or deleted is
   * 404 PRB-010; an instructor naming another author's problem is 403 PRB-003.
   */
  AuthoringProblemRecord source(UUID sourceId, UUID actorId, boolean admin) {
    // ponytail: the source is read without a lock; a testcase edited by its author mid-copy can be
    // missed. Lock the source row FOR SHARE if concurrent editing during a duplicate matters.
    var source =
        reads
            .find(sourceId, true)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    if (!admin && !source.authorId().equals(actorId)) {
      throw new PermissionDeniedException("PRB-003", "Only the author can duplicate this problem");
    }
    return source;
  }

  /** Copies the spec at version 1 and every testcase to the new problem; testcase set stays 1. */
  void copy(AuthoringProblemRecord source, UUID targetId, UUID actorId) {
    content.markDuplicatedFrom(targetId, source.id());
    if (source.spec() != null) {
      specs.save(targetId, new StoredProblemSpec(source.spec().data(), 1));
      specs.touch(targetId, actorId, source.functionWrapperSupported());
    }
    for (var t : source.testcases()) {
      testcases.insert(targetId, copyOf(t, targetId));
    }
  }

  private StoredTestcase copyOf(AuthoringProblemRecord.AuthoringTestcase t, UUID targetId) {
    var id = store.newTestcaseId();
    var input =
        store.copyOf(
            targetId,
            id,
            "input",
            new Content(t.inputStorage(), t.inputInline(), t.inputObjectKey()));
    var expected =
        store.copyOf(
            targetId,
            id,
            "expected_output",
            new Content(t.expectedStorage(), t.expectedInline(), t.expectedObjectKey()));
    return new StoredTestcase(
        id, t.visibility(), t.origin(), t.approved(), t.category(), t.position(), input, expected);
  }
}
