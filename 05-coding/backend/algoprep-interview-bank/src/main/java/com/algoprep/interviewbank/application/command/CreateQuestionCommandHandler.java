package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.application.ports.in.CreateQuestionUseCase;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateQuestionCommandHandler implements CreateQuestionUseCase {
  private final QuestionAuthoringService service;

  CreateQuestionCommandHandler(QuestionAuthoringService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public AdminQuestion execute(QuestionCommand command) {
    return service.create(command);
  }
}
