package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.LearnerQuestionQuery;
import com.algoprep.interviewbank.domain.model.study.LearnerQuestionPage;

public interface ListInterviewQuestionsUseCase {
  LearnerQuestionPage list(LearnerQuestionQuery query);
}
