package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.domain.model.CatalogItem;
import java.util.List;

public interface CreateProblemTopicUseCase {
  List<CatalogItem> execute(CatalogCommand command);
}
