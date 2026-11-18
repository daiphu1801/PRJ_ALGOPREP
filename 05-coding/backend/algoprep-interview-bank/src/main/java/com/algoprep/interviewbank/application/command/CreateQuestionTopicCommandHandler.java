package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionTopicUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateQuestionTopicCommandHandler implements CreateQuestionTopicUseCase {
  private final QuestionCatalogMutationService service;

  CreateQuestionTopicCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public CatalogItem execute(CatalogCommand command) {
    return service.create(CatalogKind.TOPIC, command);
  }
}
