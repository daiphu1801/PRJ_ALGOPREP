package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.domain.model.AdminQuestion;
import java.util.UUID;

public interface GetAdminQuestionUseCase {
  AdminQuestion get(UUID id);
}
