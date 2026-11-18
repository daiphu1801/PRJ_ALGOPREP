package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.DeleteQuestionLevelUseCase;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteQuestionLevelCommandHandler implements DeleteQuestionLevelUseCase {
  private final QuestionCatalogMutationService service;

  DeleteQuestionLevelCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public void execute(CatalogCommand command) {
    service.delete(CatalogKind.LEVEL, command);
  }
}
