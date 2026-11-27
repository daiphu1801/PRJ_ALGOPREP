package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.BulkRetireCommand;
import com.algoprep.interviewbank.application.dto.BulkRetireResult;

public interface BulkRetireQuestionsUseCase {
  BulkRetireResult execute(BulkRetireCommand command);
}
