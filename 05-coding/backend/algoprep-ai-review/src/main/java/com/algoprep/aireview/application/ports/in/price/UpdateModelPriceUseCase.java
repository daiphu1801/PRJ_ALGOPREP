package com.algoprep.aireview.application.ports.in.price;

import com.algoprep.aireview.domain.model.price.ModelPrice;
import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateModelPriceUseCase {
  /** Null arguments keep the stored value; an unknown id raises AIR-121. */
  ModelPrice update(UUID id, String modelName, BigDecimal usdPerMillionTokens);
}
