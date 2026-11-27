package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.util.List;

/**
 * One CSV row as parsed from JSON. Wrong shapes are flagged instead of rejected so that the server
 * can report them per row; null text means absent. A rubric entry may carry null parts.
 */
public record ImportRow(
    int line,
    String topic,
    String level,
    String question,
    List<RubricCriterion> rubric,
    boolean rubricShapeOk,
    List<String> followUps,
    String content,
    List<String> suggestedApproach,
    List<String> coreKeywords,
    String sampleAnswer,
    boolean fieldsShapeOk) {}
