package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.SendInterviewTurnCommand;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.UUID;

/** Body of endpoint 10 and its validation (DD validation F5: AIR-V08, AIR-V09). */
record InterviewTurnRequest(String content, Integer expectedTurnCount) {
  static final int CONTENT_MAX = 4000;

  SendInterviewTurnCommand toCommand(UUID callerId, CallerCategory category, UUID sessionId) {
    // The length is checked on the raw text: the answer is stored as sent, never trimmed or cut.
    if (content == null || content.isBlank() || content.length() > CONTENT_MAX) {
      throw new AiInvalidRequestException(
          "AIR-V08", "content must have 1 to " + CONTENT_MAX + " characters");
    }
    if (expectedTurnCount == null || expectedTurnCount < 0) {
      throw new AiInvalidRequestException(
          "AIR-V09", "expectedTurnCount is required and must not be negative");
    }
    return new SendInterviewTurnCommand(callerId, category, sessionId, content, expectedTurnCount);
  }
}
