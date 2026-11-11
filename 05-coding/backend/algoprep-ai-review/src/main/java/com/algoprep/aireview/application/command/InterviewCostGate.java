package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.CheckAiBudgetUseCase;
import com.algoprep.aireview.application.ports.out.AiCostGatePort;
import com.algoprep.aireview.domain.exception.AiRateLimitedException;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.GateOutcome;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * BR-AI-04 for a NEW interview session: budget gate (STUDENT and INSTRUCTOR), then cooldown plus
 * the {@code interview_per_hour} quota. Turns and hints never go through here (step 7).
 */
@Component
class InterviewCostGate {
  private final CheckAiBudgetUseCase budget;
  private final AiBudgetRepository config;
  private final AiCostGatePort gate;

  InterviewCostGate(CheckAiBudgetUseCase budget, AiBudgetRepository config, AiCostGatePort gate) {
    this.budget = budget;
    this.config = config;
    this.gate = gate;
  }

  /** Returns the reservation to refund on a later failure; throws AIR-401, 403 or 404. */
  CostReservation reserve(UUID userId, CallerCategory category) {
    budget.check(userId, category);
    BudgetConfig limits = config.readConfig();
    CostReservation reservation =
        gate.reserve(
            userId,
            FeatureCode.MOCK_INTERVIEW,
            limits.interviewPerHour(),
            limits.cooldownSeconds());
    if (reservation.outcome() == GateOutcome.COOLDOWN) {
      throw AiRateLimitedException.cooldown(reservation.retryAfterSeconds());
    }
    if (reservation.outcome() == GateOutcome.QUOTA_EXCEEDED) {
      throw AiRateLimitedException.interviewQuota(
          limits.interviewPerHour(), reservation.retryAfterSeconds());
    }
    return reservation;
  }

  void refund(CostReservation reservation) {
    if (reservation.granted()) {
      gate.refund(reservation);
    }
  }
}
