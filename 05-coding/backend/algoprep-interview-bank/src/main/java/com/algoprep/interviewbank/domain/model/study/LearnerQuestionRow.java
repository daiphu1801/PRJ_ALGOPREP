package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/** One list item; approach and keywords are filled for drill cards only. */
public record LearnerQuestionRow(
    UUID id,
    String topicCode,
    String topicName,
    String levelCode,
    String levelName,
    String title,
    RecallRating recall,
    boolean bookmarked,
    String suggestedApproach,
    List<String> coreKeywords) {}
