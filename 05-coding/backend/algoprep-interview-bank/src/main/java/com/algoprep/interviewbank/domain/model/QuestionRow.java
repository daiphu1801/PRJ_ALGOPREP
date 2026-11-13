package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/** A question row read under a row lock, for state checks. */
public record QuestionRow(UUID id, UUID topicId, String status, String title) {}
