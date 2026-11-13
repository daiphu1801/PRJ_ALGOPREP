package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.PracticeFeedback;

/**
 * P5: one structured LLM call with feature code PRACTICE_FEEDBACK, no retry, 30 seconds. Writes the
 * token ledger row before the answer is validated. Throws AIR-501 (provider) or AIR-502 (the answer
 * does not satisfy the rubric snapshot).
 */
public interface ComparePracticeAnswerUseCase {
  PracticeFeedback.Output compare(PracticeFeedback.Input input);
}
