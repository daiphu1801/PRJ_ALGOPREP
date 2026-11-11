package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.application.dto.InterviewTranscriptExport;
import java.util.UUID;

/**
 * Interview conversations of one user, owned by the ai-review module (P4). A failure throws; it is
 * never turned into an empty export.
 */
public interface InterviewTranscriptExportPort {
  InterviewTranscriptExport readAll(UUID userId);
}
