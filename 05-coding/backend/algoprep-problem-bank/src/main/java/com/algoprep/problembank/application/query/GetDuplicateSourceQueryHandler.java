package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.DuplicateSourceView;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.ports.in.GetDuplicateSourceUseCase;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 section 35: reads the source of a duplicate and writes nothing. A missing or soft-deleted
 * source is 404 PRB-010; an instructor naming someone else's problem is 403 PRB-003, as the DD
 * states (it does not distinguish drafts from published problems).
 */
@Service
class GetDuplicateSourceQueryHandler implements GetDuplicateSourceUseCase {
  private final ProblemAuthoringReadRepository repository;

  GetDuplicateSourceQueryHandler(ProblemAuthoringReadRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public DuplicateSourceView get(ProblemAuthoringQuery query) {
    var p =
        repository
            .find(query.problemId(), false)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    if (!query.admin() && !p.authorId().equals(query.actorId())) {
      throw new PermissionDeniedException("PRB-003", "Only the author can duplicate this problem");
    }
    return new DuplicateSourceView(
        p.title(),
        p.statementMd(),
        p.constraintsMd(),
        p.levelId(),
        p.limits(),
        p.topicIds(),
        p.tags().stream().map(AuthoringProblemRecord.Tag::name).toList(),
        p.referenceSolution(),
        withoutIds(p.workedExamples()),
        new DuplicateSourceView.WillCopyOnSave(p.testcases().size(), p.spec() != null),
        p.id());
  }

  private static List<ProblemContentData.Example> withoutIds(
      List<ProblemContentData.Example> examples) {
    return examples.stream()
        .map(
            e ->
                new ProblemContentData.Example(
                    null, e.position(), e.input(), e.output(), e.explanationMd()))
        .toList();
  }
}
