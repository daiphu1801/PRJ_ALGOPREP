package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import java.util.UUID;

public interface RateQuestionRecallUseCase {
  RecallState rate(UUID userId, UUID questionId, RecallRating rating);
}
