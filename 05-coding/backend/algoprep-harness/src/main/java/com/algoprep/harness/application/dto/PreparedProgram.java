package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.LineMap;

/** Version and matching config belong to the exact spec used to generate the payload. */
public record PreparedProgram(
    Language language,
    SubmissionModel model,
    int specVersion,
    JudgePayload payload,
    LineMap lineMap,
    String signatureText,
    MatchSpec matchSpec,
    String resultSentinel,
    int harnessInputErrorExitCode) {}
