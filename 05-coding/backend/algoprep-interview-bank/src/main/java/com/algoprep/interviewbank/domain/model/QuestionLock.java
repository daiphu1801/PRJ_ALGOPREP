package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/**
 * Topic and state of a question row read under {@code FOR SHARE} inside the writing transaction.
 */
public record QuestionLock(UUID topicId, boolean active) {}
