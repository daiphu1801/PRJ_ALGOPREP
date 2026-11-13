package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.TestcaseCommands;
import com.algoprep.problembank.application.dto.TestcaseMutationResults;
import com.algoprep.problembank.application.dto.TestcaseMutationResults.Position;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.ports.in.AuthorTestcasesUseCase;
import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 sections 42 to 45. Every operation locks the parent problem first and the testcase rows
 * after it, the order the publication kernel requires.
 */
@Service
class AuthorTestcasesCommandHandler implements AuthorTestcasesUseCase {
  static final int MAX_TESTCASES = 500;
  private static final Set<String> CATEGORIES =
      Set.of(
          "TYPICAL",
          "BOUNDARY_LOW",
          "BOUNDARY_HIGH",
          "DEGENERATE",
          "DUPLICATE",
          "SORTED",
          "EXTREME");

  private final TestcaseAuthoringSupport support;
  private final TestcaseWriteRepository testcases;
  private final TestcaseContentStore contents;
  private final PublishChecklistPolicy policy;

  AuthorTestcasesCommandHandler(
      TestcaseAuthoringSupport support,
      TestcaseWriteRepository testcases,
      TestcaseContentStore contents,
      PublishChecklistPolicy policy) {
    this.support = support;
    this.testcases = testcases;
    this.contents = contents;
    this.policy = policy;
  }

  @Override
  @Transactional
  public TestcaseMutationResults.Saved create(TestcaseCommands.Create command) {
    var problem = support.lock(command.scope(), command.problemId());
    String visibility = command.visibility() == null ? "HIDDEN" : visibility(command.visibility());
    String category = category(command.category());
    String input = content(command.input());
    String expected = content(command.expectedOutput());
    if (testcases.counts(problem.id()).total() >= MAX_TESTCASES) {
      throw new DomainRuleViolationException("PRB-046", "A problem holds at most 500 testcases");
    }
    UUID id = contents.newTestcaseId();
    var created =
        new StoredTestcase(
            id,
            visibility,
            "MANUAL",
            true,
            category,
            testcases.nextPosition(problem.id()),
            contents.place(problem.id(), id, "input", input),
            contents.place(problem.id(), id, "expected_output", expected));
    testcases.insert(problem.id(), created);
    var touch = support.settle(problem, command.scope().actorId(), true);
    return support.saved(created, touch);
  }

  @Override
  @Transactional
  public TestcaseMutationResults.Saved update(TestcaseCommands.Update command) {
    var problem = support.lock(command.scope(), command.problemId());
    var old = row(command.problemId(), command.testcaseId());
    var patch = command.patch();
    if (Boolean.FALSE.equals(patch.approved()) && old.approved()) {
      throw new InvalidRequestException("PRB-001", "An approved testcase cannot be unapproved");
    }
    String visibility =
        patch.visibility() == null ? old.visibility() : visibility(patch.visibility());
    String category = patch.categoryPresent() ? category(patch.category()) : old.category();
    String input = patch.input() == null ? null : content(patch.input());
    String expected = patch.expectedOutput() == null ? null : content(patch.expectedOutput());
    boolean approved = old.approved() || Boolean.TRUE.equals(patch.approved());
    if (old.approved() && "SAMPLE".equals(old.visibility()) && "HIDDEN".equals(visibility)) {
      var now = testcases.counts(problem.id());
      requireMinimums(
          problem, new ApprovalCounts(now.total(), now.approved(), now.approvedSamples() - 1));
    }
    var in = old.input();
    if (input != null) {
      in = contents.place(problem.id(), old.id(), "input", input);
      contents.discardAfterCommit(old.input());
    }
    var out = old.expectedOutput();
    if (expected != null) {
      out = contents.place(problem.id(), old.id(), "expected_output", expected);
      contents.discardAfterCommit(old.expectedOutput());
    }
    var changed =
        new StoredTestcase(
            old.id(), visibility, old.origin(), approved, category, old.position(), in, out);
    testcases.update(changed);
    boolean touchesGrading =
        (old.approved() || approved)
            && (patch.visibility() != null
                || input != null
                || expected != null
                || patch.approved() != null);
    var touch = support.settle(problem, command.scope().actorId(), touchesGrading);
    return support.saved(changed, touch);
  }

  @Override
  @Transactional
  public TestcaseMutationResults.Removed delete(TestcaseCommands.Delete command) {
    var problem = support.lock(command.scope(), command.problemId());
    var old = row(command.problemId(), command.testcaseId());
    if (old.approved()) {
      var now = testcases.counts(problem.id());
      requireMinimums(
          problem,
          new ApprovalCounts(
              now.total() - 1,
              now.approved() - 1,
              now.approvedSamples() - ("SAMPLE".equals(old.visibility()) ? 1 : 0)));
    }
    testcases.delete(old.id());
    contents.discardAfterCommit(old.input());
    contents.discardAfterCommit(old.expectedOutput());
    var touch = support.settle(problem, command.scope().actorId(), old.approved());
    return new TestcaseMutationResults.Removed(touch.testcaseSetVersion(), touch.updatedAt());
  }

  @Override
  @Transactional
  public TestcaseMutationResults.Ordered reorder(TestcaseCommands.Reorder command) {
    var problem = support.lock(command.scope(), command.problemId());
    List<UUID> wanted = command.orderedIds();
    if (wanted == null || wanted.contains(null)) {
      throw new InvalidRequestException("PRB-001", "orderedIds is required");
    }
    List<UUID> current = testcases.lockOrder(problem.id());
    Set<UUID> distinct = new HashSet<>(wanted);
    if (distinct.size() != wanted.size() || !distinct.equals(new HashSet<>(current))) {
      throw new DomainRuleViolationException(
          "PRB-049", "Order must contain every testcase id exactly once");
    }
    if (wanted.equals(current)) {
      var state = testcases.state(problem.id());
      return ordered(wanted, state.updatedAt(), state.testcaseSetVersion());
    }
    testcases.setPositions(wanted);
    var touch = support.settle(problem, command.scope().actorId(), true);
    return ordered(wanted, touch.updatedAt(), touch.testcaseSetVersion());
  }

  /** A published problem may not fall below the checklist minimums (Q-06): 422 PRB-054. */
  private void requireMinimums(ProblemContentSnapshot problem, ApprovalCounts after) {
    if ("PUBLISHED".equals(problem.status())
        && (after.approved() < policy.minApprovedTestcases()
            || after.approvedSamples() < policy.minApprovedSamples())) {
      throw new DomainRuleViolationException(
          "PRB-054", "A published problem needs its minimum approved testcases and samples");
    }
  }

  private StoredTestcase row(UUID problemId, UUID testcaseId) {
    return testcases
        .lock(problemId, testcaseId)
        .orElseThrow(() -> new ResourceNotFoundException("PRB-011", "Testcase not found"));
  }

  private static TestcaseMutationResults.Ordered ordered(
      List<UUID> ids, java.time.Instant updatedAt, int version) {
    var items = new ArrayList<Position>();
    for (int i = 0; i < ids.size(); i++) {
      items.add(new Position(ids.get(i), i + 1));
    }
    return new TestcaseMutationResults.Ordered(items, updatedAt, version);
  }

  static String visibility(String value) {
    if (!"SAMPLE".equals(value) && !"HIDDEN".equals(value)) {
      throw new InvalidRequestException("PRB-001", "Invalid visibility");
    }
    return value;
  }

  static String category(String value) {
    if (value != null && !CATEGORIES.contains(value)) {
      throw new InvalidRequestException("PRB-001", "Invalid category");
    }
    return value;
  }

  /** Not empty (400 PRB-001) and at most 1 MB (422 PRB-048, use the bulk upload instead). */
  private static String content(String value) {
    if (value == null || value.isEmpty()) {
      throw new InvalidRequestException("PRB-001", "Testcase content must not be empty");
    }
    if (TestcaseContentStore.tooLarge(value)) {
      throw new DomainRuleViolationException(
          "PRB-048", "Testcase content is over 1 MB; use the bulk upload");
    }
    return value;
  }
}
