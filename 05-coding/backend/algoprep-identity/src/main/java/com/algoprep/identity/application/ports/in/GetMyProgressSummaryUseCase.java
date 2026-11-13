package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.MyProgressSummary;
import java.util.UUID;

/** Personal progress read #47. */
public interface GetMyProgressSummaryUseCase {
  MyProgressSummary get(UUID userId);
}
