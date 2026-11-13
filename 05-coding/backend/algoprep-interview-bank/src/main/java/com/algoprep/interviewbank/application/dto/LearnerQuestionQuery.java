package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.RecallFilter;
import java.util.UUID;

/** Leniently parsed list parameters: bad values were already replaced by defaults. */
public record LearnerQuestionQuery(
    UUID userId,
    String q,
    RecallFilter status,
    String topic,
    String level,
    boolean bookmarkedOnly,
    int page,
    boolean drill,
    int limit) {}
