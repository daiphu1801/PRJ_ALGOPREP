package com.algoprep.aireview.application.ports.in.price;

import com.algoprep.aireview.domain.model.price.ModelPrice;
import java.util.List;

public interface ListModelPricesUseCase {
  List<ModelPrice> list();
}
