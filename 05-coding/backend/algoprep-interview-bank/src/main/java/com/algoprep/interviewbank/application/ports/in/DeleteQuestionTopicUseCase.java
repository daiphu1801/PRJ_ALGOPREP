package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.CatalogCommand;

public interface DeleteQuestionTopicUseCase {
  void execute(CatalogCommand command);
}
