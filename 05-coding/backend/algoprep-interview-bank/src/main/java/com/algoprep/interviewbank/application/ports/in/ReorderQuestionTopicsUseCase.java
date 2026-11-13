package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import java.util.List;

public interface ReorderQuestionTopicsUseCase {
  List<CatalogItem> execute(CatalogCommand command);
}
