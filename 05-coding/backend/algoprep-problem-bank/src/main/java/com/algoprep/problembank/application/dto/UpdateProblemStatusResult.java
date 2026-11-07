package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.PublishChecklist;
import java.time.Instant;

/** Response of DD F2 section 40.3. */
public record UpdateProblemStatusResult(
    String status,
    Instant publishedAt,
    int currentTestcaseSetVersion,
    PublishChecklist checklist,
    Instant problemUpdatedAt) {}
