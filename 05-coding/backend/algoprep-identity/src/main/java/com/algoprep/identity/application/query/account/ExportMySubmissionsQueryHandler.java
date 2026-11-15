package com.algoprep.identity.application.query;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.dto.SubmissionExport;
import com.algoprep.identity.application.ports.in.ExportMySubmissionsUseCase;
import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import com.algoprep.identity.application.ports.out.SubmissionExportPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The caller is the only subject: the id comes from the access token. The rate-limit slot is taken
 * first and handed back when the judge cannot answer, so a failed export does not cost the learner
 * five minutes of waiting ({@code [SoT: Suy luan]}).
 */
@Service
class ExportMySubmissionsQueryHandler implements ExportMySubmissionsUseCase {
  static final String SCOPE = "submissions";

  // ponytail: fixed cap of 2000 rows (100 history pages); a stream or a job if a learner ever
  // exceeds it.
  static final int MAX_ROWS = 2000;

  private final ExportRateLimitPort limit;
  private final SubmissionExportPort submissions;

  ExportMySubmissionsQueryHandler(ExportRateLimitPort limit, SubmissionExportPort submissions) {
    this.limit = limit;
    this.submissions = submissions;
  }

  @Override
  public SubmissionExport export(UUID userId) {
    if (!limit.tryAcquire(userId, SCOPE)) {
      throw new RateLimitExceededException(
          "IDT-V06", "This export was already requested in the last 5 minutes");
    }
    try {
      return submissions.readAll(userId, MAX_ROWS);
    } catch (RuntimeException failure) {
      limit.release(userId, SCOPE);
      throw new ServiceUnavailableException(
          IdentityErrorCodes.SUBMISSION_EXPORT_UNAVAILABLE,
          "The submission history is temporarily unavailable",
          null);
    }
  }
}
