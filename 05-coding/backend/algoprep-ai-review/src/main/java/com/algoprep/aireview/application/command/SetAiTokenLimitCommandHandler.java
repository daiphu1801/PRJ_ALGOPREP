package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.ports.in.SetAiTokenLimitUseCase;
import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.application.query.BudgetStatusAssembler;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 19 (BR-AI-05 step 8). Raising the limit never unlocks; lowering it below the used total
 * does not lock now, the next gate call does. The audit row joins this transaction (AIR-503).
 */
@Service
@Transactional
class SetAiTokenLimitCommandHandler implements SetAiTokenLimitUseCase {
  static final String ACTION = "AI_BUDGET_LIMIT_CHANGED";

  private final AiBudgetAdminRepository admin;
  private final BudgetStatusAssembler assembler;
  private final AiAuditPort audit;

  SetAiTokenLimitCommandHandler(
      AiBudgetAdminRepository admin, BudgetStatusAssembler assembler, AiAuditPort audit) {
    this.admin = admin;
    this.assembler = assembler;
    this.audit = audit;
  }

  @Override
  public BudgetStatus setLimit(UUID actor, long limitTokens) {
    long previous = admin.updateLimit(limitTokens, actor);
    try {
      audit.record(
          new AiAuditEntry(
              actor,
              ACTION,
              "AI_TOKEN_BUDGET",
              "1",
              "{\"limitTokens\":" + previous + "}",
              "{\"limitTokens\":" + limitTokens + "}"));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "AIR-503", "Audit write unavailable; change rolled back", failure);
    }
    return assembler.status();
  }
}
