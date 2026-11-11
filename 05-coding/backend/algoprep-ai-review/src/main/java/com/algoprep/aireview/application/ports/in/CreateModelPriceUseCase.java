package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.ModelPrice;

public interface CreateModelPriceUseCase {
  ModelPrice create(String modelName);
}
