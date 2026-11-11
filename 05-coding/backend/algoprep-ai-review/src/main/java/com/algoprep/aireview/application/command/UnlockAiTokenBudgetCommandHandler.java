package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.ports.in.UnlockAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.application.query.BudgetStatusAssembler;
import com.algoprep.aireview.domain.exception.AiBudgetUnlockException;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 20 (BR-AI-05 step 9): the singleton row is locked first; not locked means nothing
 * changes and nothing is audited; a total that has reached the limit is AIR-203.
 */
@Service
@Transactional
class UnlockAiTokenBudgetCommandHandler implements UnlockAiTokenBudgetUseCase {
  static final String ACTION = "AI_BUDGET_UNLOCKED";

  private final AiBudgetAdminRepository admin;
  private final AiBudgetRepository budget;
  private final BudgetStatusAssembler assembler;
  private final AiAuditPort audit;

  UnlockAiTokenBudgetCommandHandler(
      AiBudgetAdminRepository admin,
      AiBudgetRepository budget,
      BudgetStatusAssembler assembler,
      AiAuditPort audit) {
    this.admin = admin;
    this.budget = budget;
    this.assembler = assembler;
    this.audit = audit;
  }

  @Override
  public BudgetStatus unlock(UUID actor) {
    BudgetConfig config = admin.readConfigForUpdate();
    if (config.lockedAt() == null) {
      return assembler.status();
    }
    long used = budget.usedTokensSince(BudgetPolicy.periodStartAt(config.periodStart()));
    if (BudgetPolicy.exhausted(config.limitTokens(), used)) {
      throw new AiBudgetUnlockException();
    }
    admin.clearLock(actor);
    try {
      audit.record(
          new AiAuditEntry(
              actor,
              ACTION,
              "AI_TOKEN_BUDGET",
              "1",
              "{\"locked\":true,\"limitTokens\":" + config.limitTokens() + "}",
              "{\"locked\":false,\"limitTokens\":" + config.limitTokens() + "}"));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "AIR-503", "Audit write unavailable; change rolled back", failure);
    }
    return assembler.status();
  }
}
