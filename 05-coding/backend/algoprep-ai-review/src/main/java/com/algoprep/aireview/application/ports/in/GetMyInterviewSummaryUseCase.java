package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.InterviewSummary;
import java.util.UUID;

/** Endpoint 13 (DD F5 section 13): reads PostgreSQL only, so it works while the LLM is down. */
public interface GetMyInterviewSummaryUseCase {
  /** {@code range} is {@code 7d}, {@code 30d} or {@code all}; {@code limit} is 1 to 10. */
  InterviewSummary summary(UUID userId, String range, int limit);
}
