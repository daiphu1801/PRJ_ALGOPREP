package com.algoprep.interviewbank.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One immutable row of {@code user_answers}; {@code feedback} is null unless COMPLETED. */
public record AnswerAttempt(
    UUID id,
    int attemptNo,
    Instant createdAt,
    String answerText,
    FeedbackStatus status,
    Feedback feedback) {}
