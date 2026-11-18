package com.algoprep.aireview.application.dto.interview;

import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;

/** Endpoint 11 payload. */
public record InterviewHint(String hint, InterviewSessionStage stage) {}
