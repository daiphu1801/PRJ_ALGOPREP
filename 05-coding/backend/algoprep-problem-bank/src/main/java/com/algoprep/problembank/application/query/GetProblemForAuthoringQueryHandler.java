package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringView;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseSummary;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.UpdatedBy;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.policy.TestcaseViewMapper;
import com.algoprep.problembank.application.ports.in.GetProblemForAuthoringUseCase;
import com.algoprep.problembank.application.ports.out.UserDirectoryPort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * DD F2 section 34. A draft of someone else is 404, the same as a missing id; a published problem
 * of someone else is read-only with no testcase bodies and no reference solution. Testcase bodies
 * need {@code TESTCASE_MANAGEMENT:READ} and authorship (or admin); the database read itself leaves
 * them out otherwise.
 */
@Service
class GetProblemForAuthoringQueryHandler implements GetProblemForAuthoringUseCase {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final ProblemAuthoringReadRepository repository;
  private final TestcaseViewMapper testcaseViews;
  private final UserDirectoryPort users;
  private final PublishChecklistPolicy checklist;

  GetProblemForAuthoringQueryHandler(
      ProblemAuthoringReadRepository repository,
      TestcaseViewMapper testcaseViews,
      UserDirectoryPort users,
      PublishChecklistPolicy checklist) {
    this.repository = repository;
    this.testcaseViews = testcaseViews;
    this.users = users;
    this.checklist = checklist;
  }

  @Override
  @Transactional(readOnly = true)
  public ProblemAuthoringView get(ProblemAuthoringQuery query) {
    var overview =
        repository
            .find(query.problemId(), false)
            .orElseThrow(GetProblemForAuthoringQueryHandler::missing);
    boolean owner = query.admin() || overview.authorId().equals(query.actorId());
    if (!owner && !"PUBLISHED".equals(overview.status())) {
      throw missing();
    }
    boolean content = owner && query.canReadTestcases();
    var problem =
        content
            ? repository
                .find(query.problemId(), true)
                .orElseThrow(GetProblemForAuthoringQueryHandler::missing)
            : overview;
    return view(problem, !owner, content);
  }

  private ProblemAuthoringView view(AuthoringProblemRecord p, boolean readOnly, boolean content) {
    var rows = p.testcases();
    var summary =
        new TestcaseSummary(
            rows.size(),
            (int) rows.stream().filter(AuthoringTestcase::approved).count(),
            (int) rows.stream().filter(t -> "SAMPLE".equals(t.visibility())).count(),
            (int) rows.stream().filter(t -> "HIDDEN".equals(t.visibility())).count());
    List<TestcaseView> views = new ArrayList<>();
    if (content) {
      rows.forEach(row -> views.add(testcaseViews.view(row)));
    }
    return new ProblemAuthoringView(
        p.id(),
        p.code(),
        p.title(),
        p.statementMd(),
        p.constraintsMd(),
        p.levelId(),
        p.status(),
        p.limits(),
        p.topicIds(),
        p.tags(),
        readOnly ? null : p.referenceSolution(),
        p.workedExamples(),
        views,
        summary,
        spec(p.spec()),
        p.functionWrapperSupported(),
        p.currentTestcaseSetVersion(),
        new ProblemAuthoringView.AiContext(p.aiBrief(), p.noFullCode(), p.socraticOnly()),
        new ProblemAuthoringView.AiGeneration(
            p.aiGenerationsUsed(), repository.aiGenerationLimit()),
        checklist.evaluate(p, null),
        p.authorId(),
        p.updatedAt(),
        updatedBy(p.updatedBy()),
        p.duplicatedFromProblemId(),
        readOnly);
  }

  private UpdatedBy updatedBy(UUID userId) {
    if (userId == null) {
      return null;
    }
    String name;
    try {
      name = users.getDisplayNames(Set.of(userId)).get(userId);
    } catch (RuntimeException failure) {
      name = null;
    }
    return new UpdatedBy(userId, name);
  }

  private static JsonNode spec(StoredProblemSpec stored) {
    if (stored == null) {
      return null;
    }
    var d = stored.data();
    ObjectNode node = JSON.createObjectNode();
    node.put("matchingStrategy", d.matchingStrategy());
    node.put("epsilonValue", d.epsilonValue());
    node.put("stdinFormatMd", d.stdinFormatMd());
    node.put("stdoutFormatMd", d.stdoutFormatMd());
    node.put("functionName", d.functionName());
    node.set("returnType", json(d.returnTypeJson()));
    node.set("parameters", json(d.parametersJson()));
    node.set("nameOverrides", json(d.nameOverridesJson()));
    node.put("specVersion", stored.specVersion());
    return node;
  }

  private static JsonNode json(String text) {
    return text == null ? JSON.nullNode() : JSON.readTree(text);
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}
