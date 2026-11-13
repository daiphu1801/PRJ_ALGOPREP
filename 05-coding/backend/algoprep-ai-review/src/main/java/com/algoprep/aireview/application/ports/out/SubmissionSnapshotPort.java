package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import java.util.UUID;

/**
 * Outbound port served by judge-orchestration (DD F5 section 1.3). The serving side checks no
 * ownership: the use case compares {@code userId}.
 */
public interface SubmissionSnapshotPort {
  /**
   * @throws com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException AIR-101
   * @throws com.algoprep.aireview.domain.exception.AiSubmissionNotAcceptedException AIR-102
   * @throws com.algoprep.aireview.domain.exception.AiProviderUnavailableException UPSTREAM_PORT on
   *     an infrastructure failure
   */
  SubmissionSnapshot getAccepted(UUID submissionId);
}
