package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.BulkRetireCommand;
import com.algoprep.interviewbank.application.dto.BulkRetireResult;
import com.algoprep.interviewbank.application.ports.in.BulkRetireQuestionsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BulkRetireQuestionsCommandHandler implements BulkRetireQuestionsUseCase {
  private final QuestionAuthoringService service;

  BulkRetireQuestionsCommandHandler(QuestionAuthoringService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public BulkRetireResult execute(BulkRetireCommand command) {
    return service.retire(command);
  }
}
