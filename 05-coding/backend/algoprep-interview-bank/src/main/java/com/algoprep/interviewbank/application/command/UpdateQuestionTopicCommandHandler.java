package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.UpdateQuestionTopicUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateQuestionTopicCommandHandler implements UpdateQuestionTopicUseCase {
  private final QuestionCatalogMutationService service;

  UpdateQuestionTopicCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public CatalogItem execute(CatalogCommand command) {
    return service.update(CatalogKind.TOPIC, command);
  }
}
