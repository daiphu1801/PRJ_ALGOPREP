package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.application.dto.PracticeOutcome;
import com.algoprep.interviewbank.application.dto.PracticeSubmission;

/** Endpoint 6 (DD api F6 section 6). Not idempotent: every call is a new attempt. */
public interface SubmitPracticeAnswerUseCase {
  PracticeOutcome submit(PracticeSubmission submission);
}
