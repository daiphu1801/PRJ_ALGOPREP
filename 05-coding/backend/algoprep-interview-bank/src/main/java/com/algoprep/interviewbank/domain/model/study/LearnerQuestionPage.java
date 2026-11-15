package com.algoprep.interviewbank.domain.model;

import java.util.List;

/**
 * {@code shown} counts the ACTIVE questions matching the filters, {@code total} all ACTIVE ones.
 */
public record LearnerQuestionPage(
    List<LearnerQuestionRow> items, int number, int size, long shown, long total) {}
