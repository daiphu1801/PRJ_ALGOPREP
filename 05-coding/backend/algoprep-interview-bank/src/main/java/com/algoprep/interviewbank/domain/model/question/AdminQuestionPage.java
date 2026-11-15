package com.algoprep.interviewbank.domain.model;

import java.util.List;

/** {@code shown} counts the filtered set, {@code total} every ACTIVE question. */
public record AdminQuestionPage(
    List<AdminQuestionRow> items, int number, int size, long shown, long total) {}
