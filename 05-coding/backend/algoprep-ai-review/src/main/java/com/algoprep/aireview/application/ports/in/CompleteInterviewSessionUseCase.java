package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.InterviewResult;
import java.util.UUID;

/** Endpoint 12 (DD F5 section 12, BR-AI-09). Idempotent: a COMPLETED session returns its rubric. */
public interface CompleteInterviewSessionUseCase {
  InterviewResult complete(UUID callerId, UUID sessionId);
}
