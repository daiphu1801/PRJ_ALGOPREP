package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.AnswerAttempt;

/** The stored attempt; {@code feedbackErrorCode} (INB-501 or INB-502) is not stored. */
public record PracticeOutcome(AnswerAttempt attempt, String feedbackErrorCode) {}
