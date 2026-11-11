package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.AlertReview;
import java.util.UUID;

/** Endpoint 29: raises AIR-120 for an unknown alert; a second call is a 200 with the flag. */
public interface MarkAiUsageAlertReviewedUseCase {
  AlertReview review(UUID alertId);
}
