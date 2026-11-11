package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.InterviewSessionStage;

/** Endpoint 11 payload. */
public record InterviewHint(String hint, InterviewSessionStage stage) {}
