package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.PracticeFeedback;
import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.UUID;

/**
 * P5 (DD F6 BR-IB-08): hourly quota first, then the shared token budget. A rejected quota returns a
 * ticket with status QUOTA_EXCEEDED. A locked budget refunds the quota and throws {@code
 * AiBudgetExceededException}; any other failure of the gate refunds and throws AIR-501.
 */
public interface ReservePracticeFeedbackUseCase {
  PracticeFeedback.Ticket reserve(UUID userId, CallerCategory category, int hourlyLimit);
}
