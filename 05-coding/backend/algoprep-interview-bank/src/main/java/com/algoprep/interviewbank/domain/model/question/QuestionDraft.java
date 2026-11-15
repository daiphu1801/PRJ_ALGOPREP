package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Validated content of a question. The three CSV-only columns are null for form-created questions
 * and are never touched by an update.
 */
public record QuestionDraft(
    UUID topicId,
    UUID levelId,
    String title,
    String contentMarkdown,
    List<String> followUps,
    List<RubricCriterion> rubric,
    String suggestedApproach,
    List<String> coreKeywords,
    String sampleAnswerFramework) {}
