package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.application.ports.in.UpdateQuestionUseCase;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateQuestionCommandHandler implements UpdateQuestionUseCase {
  private final QuestionAuthoringService service;

  UpdateQuestionCommandHandler(QuestionAuthoringService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public AdminQuestion execute(QuestionCommand command) {
    return service.update(command);
  }
}
