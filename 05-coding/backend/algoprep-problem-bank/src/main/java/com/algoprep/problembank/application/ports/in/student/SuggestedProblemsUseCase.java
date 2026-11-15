package com.algoprep.problembank.application.ports.in.student;

import com.algoprep.problembank.application.dto.student.SuggestedProblemViews.SuggestedProblems;
import java.util.Map;
import java.util.UUID;

/** Up to seven problems ranked by the caller's weakest topics (DD F2 section 13). */
public interface SuggestedProblemsUseCase {
  SuggestedProblems suggest(UUID userId, Map<String, String> params);
}
