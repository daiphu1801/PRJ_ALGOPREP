package com.algoprep.identity.application.query;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.dto.InterviewTranscriptExport;
import com.algoprep.identity.application.ports.in.ExportMyInterviewTranscriptsUseCase;
import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import com.algoprep.identity.application.ports.out.InterviewTranscriptExportPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The caller is the only subject: the id comes from the access token. Same slot discipline as the
 * submission export: the slot is taken first and handed back when the interview module cannot
 * answer, so a failed export can be retried at once. The rest of identity never depends on it.
 */
@Service
class ExportMyInterviewTranscriptsQueryHandler implements ExportMyInterviewTranscriptsUseCase {
  static final String SCOPE = "interview-transcripts";

  private final ExportRateLimitPort limit;
  private final InterviewTranscriptExportPort transcripts;

  ExportMyInterviewTranscriptsQueryHandler(
      ExportRateLimitPort limit, InterviewTranscriptExportPort transcripts) {
    this.limit = limit;
    this.transcripts = transcripts;
  }

  @Override
  public InterviewTranscriptExport export(UUID userId) {
    if (!limit.tryAcquire(userId, SCOPE)) {
      throw new RateLimitExceededException(
          "IDT-V06", "This export was already requested in the last 5 minutes");
    }
    try {
      return transcripts.readAll(userId);
    } catch (RuntimeException failure) {
      limit.release(userId, SCOPE);
      throw new ServiceUnavailableException(
          IdentityErrorCodes.TRANSCRIPT_EXPORT_UNAVAILABLE,
          "The interview conversations are temporarily unavailable",
          null);
    }
  }
}
