package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.ImportCommand;
import com.algoprep.interviewbank.application.dto.ImportResult;

public interface ImportQuestionsUseCase {
  ImportResult execute(ImportCommand command);
}
