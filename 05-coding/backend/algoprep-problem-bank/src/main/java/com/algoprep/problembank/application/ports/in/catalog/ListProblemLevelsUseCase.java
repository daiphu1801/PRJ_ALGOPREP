package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.domain.model.CatalogItem;
import java.util.List;

public interface ListProblemLevelsUseCase {
  List<CatalogItem> list(CatalogQuery query);
}
