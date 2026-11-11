package com.algoprep.interviewbank.application.ports.in;

import java.util.UUID;

public interface SetQuestionBookmarkUseCase {
  void set(UUID userId, UUID questionId, boolean bookmarked);
}
