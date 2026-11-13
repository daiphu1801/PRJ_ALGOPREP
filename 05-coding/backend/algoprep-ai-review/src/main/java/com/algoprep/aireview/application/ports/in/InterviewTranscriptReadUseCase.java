package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.InterviewTranscript;
import java.util.List;
import java.util.UUID;

/**
 * P4 (DD F5 section 32.3), called by identity for the transcript export. Reads PostgreSQL only, so
 * it works while the LLM is down. The user id is the only filter: nothing of another user's
 * sessions can come back.
 */
public interface InterviewTranscriptReadUseCase {
  /** Every session of {@code userId} (ABANDONED included) by {@code started_at}, oldest first. */
  List<InterviewTranscript> listForUser(UUID userId);
}
