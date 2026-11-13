package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.ModelPrice;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code ai_model_prices}; a duplicate name raises AiModelPriceConflictException. */
public interface AiModelPriceRepository {
  List<ModelPrice> list();

  ModelPrice create(String modelName);

  /** Updates the non-null columns; empty when the id does not exist. */
  Optional<ModelPrice> update(UUID id, String modelName, BigDecimal usdPerMillionTokens);

  void delete(UUID id);
}
