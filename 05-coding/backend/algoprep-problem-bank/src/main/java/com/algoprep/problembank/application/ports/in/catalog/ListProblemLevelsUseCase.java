package com.algoprep.problembank.application.ports.in.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogQuery;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import java.util.List;

public interface ListProblemLevelsUseCase {
  List<CatalogItem> list(CatalogQuery query);
}
