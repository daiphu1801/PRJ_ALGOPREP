package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.domain.exception.TestcaseGenerationRejectedException;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import com.algoprep.problembank.domain.ports.out.TestcaseGenerationRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-16 step 9: one transaction for the draft testcases, the run record and the quota counter. It
 * goes through the same lock, content store and position rules as manual authoring; the only
 * difference is the row it writes (origin AI, not approved, HIDDEN), which never touches grading.
 */
@Component
class GeneratedTestcaseWriter {
  /** One pair ready to store: the generated input and the real output of the sample solution. */
  record Kept(String category, String input, String output) {}

  /** What the transaction produced. */
  record Persisted(List<TestcaseView> testcases, ProblemTouch touch) {}

  /** Everything about the run besides the pairs. */
  record RunInfo(
      UUID requestedBy,
      TestcaseGeneration.Scripts scripts,
      long seed,
      List<TestcaseGeneration.Dropped> dropped,
      int used,
      int limit) {}

  private final TestcaseAuthoringSupport support;
  private final TestcaseWriteRepository testcases;
  private final TestcaseContentStore contents;
  private final TestcaseGenerationRepository generation;
  private final JsonMapper json = JsonMapper.builder().build();

  GeneratedTestcaseWriter(
      TestcaseAuthoringSupport support,
      TestcaseWriteRepository testcases,
      TestcaseContentStore contents,
      TestcaseGenerationRepository generation) {
    this.support = support;
    this.testcases = testcases;
    this.contents = contents;
    this.generation = generation;
  }

  @Transactional
  Persisted persist(AuthoringScope scope, UUID problemId, List<Kept> kept, RunInfo run) {
    var problem = support.lock(scope, problemId);
    if (!generation.consumeQuota(problemId)) {
      throw TestcaseGenerationRejectedException.quota(run.limit(), run.limit());
    }
    if (testcases.counts(problemId).total() + kept.size()
        > AuthorTestcasesCommandHandler.MAX_TESTCASES) {
      throw new DomainRuleViolationException("PRB-046", "A problem holds at most 500 testcases");
    }
    int position = testcases.nextPosition(problemId);
    var views = new ArrayList<TestcaseView>();
    for (Kept one : kept) {
      UUID id = contents.newTestcaseId();
      var stored =
          new StoredTestcase(
              id,
              "HIDDEN",
              "AI",
              false,
              one.category(),
              position++,
              contents.place(problemId, id, "input", one.input()),
              contents.place(problemId, id, "expected_output", one.output()));
      testcases.insert(problemId, stored);
      views.add(support.view(stored));
    }
    generation.insertRun(
        new TestcaseGenerationRun(
            UUID.randomUUID(),
            problemId,
            run.scripts().generatorScript(),
            run.scripts().validatorScript(),
            run.seed(),
            run.scripts().modelId(),
            run.scripts().promptVersion(),
            run.requestedBy(),
            kept.size(),
            json.writeValueAsString(run.dropped())));
    return new Persisted(views, support.settle(problem, scope.actorId(), false));
  }
}
