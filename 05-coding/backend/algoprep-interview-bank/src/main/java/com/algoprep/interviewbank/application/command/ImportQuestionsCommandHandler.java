package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.ImportCommand;
import com.algoprep.interviewbank.application.dto.ImportResult;
import com.algoprep.interviewbank.application.ports.in.ImportQuestionsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ImportQuestionsCommandHandler implements ImportQuestionsUseCase {
  private final QuestionImportService service;

  ImportQuestionsCommandHandler(QuestionImportService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public ImportResult execute(ImportCommand command) {
    return service.run(command);
  }
}
