package com.algoprep.aireview.application.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The application.yml constants of JOB-AI-04 (DD jobs F5 section 4); the defaults are the DD's. */
@Component
class InterviewSweepLimits {
  private final int idleMinutes;
  private final int enoughTurns;
  private final int maxScoreRetries;
  private final int batchSize;

  InterviewSweepLimits(
      @Value("${ai.interview.sweep.idle-minutes:15}") int idleMinutes,
      @Value("${ai.interview.sweep.enough-turns:4}") int enoughTurns,
      @Value("${ai.interview.sweep.score-max-retries:3}") int maxScoreRetries,
      @Value("${ai.interview.sweep.batch-size:20}") int batchSize) {
    this.idleMinutes = idleMinutes;
    this.enoughTurns = enoughTurns;
    this.maxScoreRetries = maxScoreRetries;
    this.batchSize = batchSize;
  }

  int idleMinutes() {
    return idleMinutes;
  }

  int enoughTurns() {
    return enoughTurns;
  }

  int maxScoreRetries() {
    return maxScoreRetries;
  }

  int batchSize() {
    return batchSize;
  }
}
