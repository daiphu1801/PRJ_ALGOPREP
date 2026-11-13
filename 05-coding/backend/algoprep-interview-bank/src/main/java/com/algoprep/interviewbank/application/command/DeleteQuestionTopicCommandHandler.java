package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.DeleteQuestionTopicUseCase;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteQuestionTopicCommandHandler implements DeleteQuestionTopicUseCase {
  private final QuestionCatalogMutationService service;

  DeleteQuestionTopicCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public void execute(CatalogCommand command) {
    service.delete(CatalogKind.TOPIC, command);
  }
}
