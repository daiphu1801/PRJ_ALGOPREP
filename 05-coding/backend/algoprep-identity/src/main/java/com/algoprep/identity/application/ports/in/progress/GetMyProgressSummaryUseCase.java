package com.algoprep.identity.application.ports.in.progress;

import com.algoprep.identity.domain.model.progress.MyProgressSummary;
import java.util.UUID;

/** Personal progress read #47. */
public interface GetMyProgressSummaryUseCase {
  MyProgressSummary get(UUID userId);
}
