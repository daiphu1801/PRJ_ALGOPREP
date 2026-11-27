package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogItem;

public interface UpdateQuestionLevelUseCase {
  CatalogItem execute(CatalogCommand command);
}
