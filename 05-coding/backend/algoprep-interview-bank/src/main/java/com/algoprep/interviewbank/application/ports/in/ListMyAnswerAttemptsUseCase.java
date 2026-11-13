package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.domain.model.AttemptPage;
import java.util.UUID;

/** Endpoint 7 (DD api F6 section 7): only the caller's own attempts, cursor {@code before}. */
public interface ListMyAnswerAttemptsUseCase {
  AttemptPage list(UUID userId, UUID questionId, Integer before);
}
