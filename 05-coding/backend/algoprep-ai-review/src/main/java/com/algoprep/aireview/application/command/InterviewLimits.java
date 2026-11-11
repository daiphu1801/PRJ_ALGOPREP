package com.algoprep.aireview.application.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The two {@code application.yml} constants of the interview flow (DD logic F5 Bang 2). */
@Component
class InterviewLimits {
  private final int maxHintsPerSession;
  private final int stageTurnCeiling;

  InterviewLimits(
      @Value("${ai.interview.max-hints-per-session:3}") int maxHintsPerSession,
      @Value("${ai.interview.stage-turn-ceiling:6}") int stageTurnCeiling) {
    this.maxHintsPerSession = maxHintsPerSession;
    this.stageTurnCeiling = stageTurnCeiling;
  }

  int maxHintsPerSession() {
    return maxHintsPerSession;
  }

  int stageTurnCeiling() {
    return stageTurnCeiling;
  }
}
