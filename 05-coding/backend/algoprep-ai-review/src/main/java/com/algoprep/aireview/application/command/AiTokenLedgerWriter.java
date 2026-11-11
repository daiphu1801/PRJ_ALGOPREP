package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.model.TokenUsageRecord;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.aireview.domain.ports.out.AiTokenUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One ledger attempt in its OWN transaction (REQUIRES_NEW): a rollback in the business transaction
 * of the caller cannot remove the usage row (BR-AI-01). A separate bean so the proxy applies; the
 * retry loop lives in the handler, one transaction per attempt.
 */
@Service
class AiTokenLedgerWriter {
  private final AiTokenUsageRepository usage;
  private final AiBudgetRepository budget;

  AiTokenLedgerWriter(AiTokenUsageRepository usage, AiBudgetRepository budget) {
    this.usage = usage;
    this.budget = budget;
  }

  /**
   * Inserts the row, then re-evaluates the budget lock in the same transaction (BR-AI-05 step 4).
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  boolean write(RecordAiTokenUsageCommand command) {
    boolean inserted =
        usage.insertIfAbsent(
            new TokenUsageRecord(
                command.callId(),
                command.userId(),
                command.featureCode(),
                command.problemId(),
                command.ownerId(),
                command.tokensPrompt(),
                command.tokensCompletion(),
                command.modelName()));
    if (inserted) {
      BudgetConfig config = budget.readConfig();
      if (config.lockedAt() == null
          && BudgetPolicy.exhausted(
              config.limitTokens(),
              budget.usedTokensSince(BudgetPolicy.periodStartAt(config.periodStart())))) {
        budget.lockIfUnlocked();
      }
    }
    return inserted;
  }
}
