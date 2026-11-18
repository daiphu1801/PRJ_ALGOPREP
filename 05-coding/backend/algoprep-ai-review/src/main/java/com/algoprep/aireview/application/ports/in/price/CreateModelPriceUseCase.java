package com.algoprep.aireview.application.ports.in.price;

import com.algoprep.aireview.domain.model.price.ModelPrice;

public interface CreateModelPriceUseCase {
  ModelPrice create(String modelName);
}
