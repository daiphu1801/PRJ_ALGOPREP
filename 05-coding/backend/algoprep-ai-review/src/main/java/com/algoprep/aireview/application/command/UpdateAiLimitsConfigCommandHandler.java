package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.UpdateAiLimitsConfigUseCase;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoint 22. No admin audit row by design (DD api F5 Q9); Redis counters stay untouched. */
@Service
@Transactional
class UpdateAiLimitsConfigCommandHandler implements UpdateAiLimitsConfigUseCase {
  private final AiBudgetAdminRepository admin;

  UpdateAiLimitsConfigCommandHandler(AiBudgetAdminRepository admin) {
    this.admin = admin;
  }

  @Override
  public LimitsConfig update(UUID actor, LimitsPatch patch) {
    return admin.updateLimits(patch, actor);
  }
}
