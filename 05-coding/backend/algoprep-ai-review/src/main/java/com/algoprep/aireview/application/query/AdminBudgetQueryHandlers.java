package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.ports.in.GetAiLimitsConfigUseCase;
import com.algoprep.aireview.application.ports.in.GetAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.in.ListModelPricesUseCase;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoints 18, 21 and 23: stored values only, they work while the LLM is down. */
@Service
@Transactional(readOnly = true)
class AdminBudgetQueryHandlers
    implements GetAiTokenBudgetUseCase, GetAiLimitsConfigUseCase, ListModelPricesUseCase {
  private final BudgetStatusAssembler assembler;
  private final AiBudgetAdminRepository admin;
  private final AiModelPriceRepository prices;

  AdminBudgetQueryHandlers(
      BudgetStatusAssembler assembler,
      AiBudgetAdminRepository admin,
      AiModelPriceRepository prices) {
    this.assembler = assembler;
    this.admin = admin;
    this.prices = prices;
  }

  @Override
  public BudgetStatus status() {
    return assembler.status();
  }

  @Override
  public LimitsConfig read() {
    return admin.readLimits();
  }

  @Override
  public List<ModelPrice> list() {
    return prices.list();
  }
}
