package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.QuestionCommand;
import com.algoprep.interviewbank.domain.model.AdminQuestion;

public interface CreateQuestionUseCase {
  AdminQuestion execute(QuestionCommand command);
}
