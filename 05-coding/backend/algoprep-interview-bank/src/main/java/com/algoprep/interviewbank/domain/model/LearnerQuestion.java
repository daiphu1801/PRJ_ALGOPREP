package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/** Study-mode view of an ACTIVE question, already scoped to one learner. */
public record LearnerQuestion(
    UUID id,
    String topicCode,
    String topicName,
    boolean usesStarFramework,
    String levelCode,
    String levelName,
    String title,
    String contentMarkdown,
    String suggestedApproach,
    String sampleAnswerFramework,
    List<String> coreKeywords,
    List<String> followUps,
    boolean bookmarked,
    boolean practiceAvailable,
    RecallState recall) {}
