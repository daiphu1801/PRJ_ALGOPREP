package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.InterviewSessionDetail;
import java.util.UUID;

/** Endpoint 9 (DD F5 section 9). Reads stored data only, so it works while the LLM is down. */
public interface GetInterviewSessionUseCase {
  /**
   * @throws com.algoprep.aireview.domain.exception.AiSessionNotFoundException AIR-110
   * @throws com.algoprep.aireview.domain.exception.AiNotOwnerException AIR-302
   */
  InterviewSessionDetail get(UUID callerId, UUID sessionId);
}
