package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.ModelPrice;
import java.util.List;

public interface ListModelPricesUseCase {
  List<ModelPrice> list();
}
