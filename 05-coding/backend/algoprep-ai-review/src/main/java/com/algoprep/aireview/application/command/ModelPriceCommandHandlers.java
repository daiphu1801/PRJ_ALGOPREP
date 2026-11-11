package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.CreateModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.DeleteModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.UpdateModelPriceUseCase;
import com.algoprep.aireview.domain.exception.AiModelPriceNotFoundException;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoints 24 to 26. No admin audit row by design (DD api F5 Q9). */
@Service
@Transactional
class ModelPriceCommandHandlers
    implements CreateModelPriceUseCase, UpdateModelPriceUseCase, DeleteModelPriceUseCase {
  private final AiModelPriceRepository prices;

  ModelPriceCommandHandlers(AiModelPriceRepository prices) {
    this.prices = prices;
  }

  @Override
  public ModelPrice create(String modelName) {
    return prices.create(modelName);
  }

  @Override
  public ModelPrice update(UUID id, String modelName, BigDecimal usdPerMillionTokens) {
    return prices
        .update(id, modelName, usdPerMillionTokens)
        .orElseThrow(AiModelPriceNotFoundException::new);
  }

  @Override
  public void delete(UUID id) {
    prices.delete(id);
  }
}
