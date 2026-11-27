package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.CatalogQuery;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import java.util.List;

public interface ListQuestionTopicsUseCase {
  List<CatalogItem> list(CatalogQuery query);
}
