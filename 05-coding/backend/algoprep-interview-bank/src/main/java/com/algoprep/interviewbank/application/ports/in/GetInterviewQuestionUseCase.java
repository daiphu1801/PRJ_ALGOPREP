package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import java.util.UUID;

public interface GetInterviewQuestionUseCase {
  LearnerQuestion get(UUID userId, UUID questionId);
}
