package com.algoprep.aireview.application.ports.in.interview;

import com.algoprep.aireview.application.dto.interview.SweepOutcome;

/** JOB-AI-04: scores or abandons running sessions nobody touched for the idle window. */
public interface SweepIdleInterviewSessionsUseCase {
  SweepOutcome sweep();
}
