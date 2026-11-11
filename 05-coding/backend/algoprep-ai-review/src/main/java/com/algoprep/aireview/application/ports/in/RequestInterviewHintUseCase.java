package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.InterviewHint;
import java.util.UUID;

/** Endpoint 11 (DD F5 section 11, BR-AI-08). One LLM call per request, not idempotent. */
public interface RequestInterviewHintUseCase {
  InterviewHint hint(UUID callerId, UUID sessionId);
}
