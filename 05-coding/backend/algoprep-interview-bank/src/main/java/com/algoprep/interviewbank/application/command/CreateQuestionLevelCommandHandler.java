package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionLevelUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateQuestionLevelCommandHandler implements CreateQuestionLevelUseCase {
  private final QuestionCatalogMutationService service;

  CreateQuestionLevelCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public CatalogItem execute(CatalogCommand command) {
    return service.create(CatalogKind.LEVEL, command);
  }
}
