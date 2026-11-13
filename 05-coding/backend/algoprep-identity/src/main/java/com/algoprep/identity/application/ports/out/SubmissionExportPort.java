package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.application.dto.SubmissionExport;
import java.util.UUID;

/**
 * The submission history of one user, owned by the judge module (F4 history port). A failure
 * throws; it is never turned into an empty or partial export.
 */
public interface SubmissionExportPort {

  /**
   * @param userId the caller, taken from the access token
   * @param maxRows cap on the rows read; the result says when more existed
   */
  SubmissionExport readAll(UUID userId, int maxRows);
}
