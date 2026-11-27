package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.LineMap;

/** Pure packaging output. This is deliberately independent of the deferred F2/P2 reader. */
public record PackagedProgram(
    Language language,
    SubmissionModel model,
    JudgePayload payload,
    LineMap lineMap,
    String signatureText,
    String resultSentinel) {}
