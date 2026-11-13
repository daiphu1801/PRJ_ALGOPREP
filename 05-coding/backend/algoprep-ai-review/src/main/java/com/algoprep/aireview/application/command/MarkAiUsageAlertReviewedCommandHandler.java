package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.MarkAiUsageAlertReviewedUseCase;
import com.algoprep.aireview.domain.exception.AiUsageAlertNotFoundException;
import com.algoprep.aireview.domain.model.AlertReview;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 29: writes only status and reviewed_at, touches no account and writes no audit row (DD
 * F5 Q9: only prompt publish, budget unlock and limit change are audited).
 */
@Service
@Transactional
class MarkAiUsageAlertReviewedCommandHandler implements MarkAiUsageAlertReviewedUseCase {
  private final AiUsageRepository usage;

  MarkAiUsageAlertReviewedCommandHandler(AiUsageRepository usage) {
    this.usage = usage;
  }

  @Override
  public AlertReview review(UUID alertId) {
    return usage.markReviewed(alertId).orElseThrow(AiUsageAlertNotFoundException::new);
  }
}
