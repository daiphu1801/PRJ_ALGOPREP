package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything DD F2 section 34 needs from the database in one read. Testcase bodies and object keys
 * are null unless the caller asked for content: the privacy decision is made before the SQL, never
 * after it.
 */
public record AuthoringProblemRecord(
    UUID id,
    String code,
    String title,
    String statementMd,
    String constraintsMd,
    UUID levelId,
    String status,
    boolean functionWrapperSupported,
    ProblemContentData.Limits limits,
    int currentTestcaseSetVersion,
    int aiGenerationsUsed,
    String aiBrief,
    boolean noFullCode,
    boolean socraticOnly,
    UUID authorId,
    UUID updatedBy,
    UUID duplicatedFromProblemId,
    Instant updatedAt,
    Instant publishedAt,
    List<UUID> topicIds,
    List<Tag> tags,
    ProblemContentData.Solution referenceSolution,
    List<ProblemContentData.Example> workedExamples,
    List<AuthoringTestcase> testcases,
    StoredProblemSpec spec) {

  public AuthoringProblemRecord {
    topicIds = List.copyOf(topicIds);
    tags = List.copyOf(tags);
    workedExamples = List.copyOf(workedExamples);
    testcases = List.copyOf(testcases);
  }

  /** A tag attached to the problem. */
  public record Tag(UUID id, String name) {}

  /**
   * One testcase row. {@code inputInline}, {@code expectedInline} and the two object keys are null
   * when the read did not ask for content.
   */
  public record AuthoringTestcase(
      UUID id,
      int position,
      String visibility,
      String origin,
      boolean approved,
      String category,
      String inputStorage,
      String inputInline,
      String inputObjectKey,
      String expectedStorage,
      String expectedInline,
      String expectedObjectKey) {}
}
