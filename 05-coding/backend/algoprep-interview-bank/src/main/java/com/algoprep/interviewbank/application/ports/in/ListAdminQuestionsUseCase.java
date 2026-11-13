package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.AdminQuestionQuery;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;

public interface ListAdminQuestionsUseCase {
  AdminQuestionPage list(AdminQuestionQuery query);
}
