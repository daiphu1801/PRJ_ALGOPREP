package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.domain.model.AuthoringProblemRecord.Tag;
import com.algoprep.problembank.domain.model.ProblemContentData;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * {@code ProblemAuthoringDetailDto} of DD F2 section 1.5, the response of sections 34, 36 and 37.
 * Field names are the wire names.
 */
public record ProblemAuthoringView(
    UUID id,
    String code,
    String title,
    String statementMd,
    String constraintsMd,
    UUID levelId,
    String status,
    ProblemContentData.Limits limits,
    List<UUID> topicIds,
    List<Tag> tags,
    ProblemContentData.Solution referenceSolution,
    List<ProblemContentData.Example> workedExamples,
    List<TestcaseView> testcases,
    TestcaseSummary testcaseSummary,
    JsonNode spec,
    boolean functionWrapperSupported,
    int currentTestcaseSetVersion,
    AiContext aiContext,
    AiGeneration aiGeneration,
    PublishChecklist checklist,
    UUID authorId,
    Instant updatedAt,
    UpdatedBy updatedBy,
    UUID duplicatedFromProblemId,
    boolean readOnly) {

  /** Testcase for the author or an admin who may read testcases; never built for a student. */
  public record TestcaseView(
      UUID id,
      int position,
      String visibility,
      String origin,
      boolean approved,
      String category,
      String input,
      String inputStorage,
      long inputSizeBytes,
      String inputDownloadUrl,
      String expectedOutput,
      String expectedOutputStorage,
      long expectedOutputSizeBytes,
      String expectedOutputDownloadUrl) {}

  /** Counts that stand in for the testcase list when the caller may not read the bodies. */
  public record TestcaseSummary(int total, int approved, int sample, int hidden) {}

  /** Authoring notes for the AI helper. */
  public record AiContext(String briefText, boolean noFullCode, boolean questionOnly) {}

  /** Testcase-generation quota: {@code limit} comes from {@code problem_bank_settings}. */
  public record AiGeneration(int used, int limit) {}

  /** Last editor; {@code displayName} is null when the user directory could not answer. */
  public record UpdatedBy(UUID userId, String displayName) {}

  /**
   * Publish checklist (BR-07). {@code sampleSolutionAllPass} and the matching item are null on a
   * read: the sandbox only runs when the status changes.
   */
  public record PublishChecklist(
      List<Item> items,
      int testcaseCount,
      int sampleTestcaseCount,
      int workedExampleCount,
      boolean specDeclared,
      Boolean sampleSolutionAllPass) {

    /** One condition; {@code required} and {@code actual} are null for the non-counting ones. */
    public record Item(String code, Boolean met, Integer required, Integer actual) {}
  }
}
