package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.domain.model.ProblemContentData;
import java.util.List;
import java.util.UUID;

/**
 * Response of DD F2 section 35: the content a new problem is pre-filled with. No id, code, status,
 * testcases or spec; those two are copied by the server on the first save (BR-10).
 */
public record DuplicateSourceView(
    String title,
    String statementMd,
    String constraintsMd,
    UUID levelId,
    ProblemContentData.Limits limits,
    List<UUID> topicIds,
    List<String> tags,
    ProblemContentData.Solution referenceSolution,
    List<ProblemContentData.Example> workedExamples,
    WillCopyOnSave willCopyOnSave,
    UUID duplicatedFromProblemId) {

  /** What the first save will copy besides the content above. */
  public record WillCopyOnSave(int testcaseCount, boolean hasSpec) {}
}
