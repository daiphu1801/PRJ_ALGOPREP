package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.OpenInterviewSessionCommand;
import com.algoprep.aireview.application.dto.OpenedInterviewSession;

/** Endpoint 8 (DD F5 section 8, BR-AI-06). Not idempotent: every call opens a new session. */
public interface OpenInterviewSessionUseCase {
  OpenedInterviewSession open(OpenInterviewSessionCommand command);
}
