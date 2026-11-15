package com.algoprep.problembank.application.ports.in.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogCommand;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import java.util.List;

public interface UpdateProblemTopicUseCase {
  List<CatalogItem> execute(CatalogCommand command);
}
