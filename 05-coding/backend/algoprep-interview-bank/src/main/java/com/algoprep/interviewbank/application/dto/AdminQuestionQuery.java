package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.QuestionSort;

/** Strictly validated list parameters; {@code topic} and {@code level} are catalogue codes. */
public record AdminQuestionQuery(
    String q,
    String topic,
    String level,
    QuestionSort sort,
    boolean descending,
    int page,
    int pageSize) {}
