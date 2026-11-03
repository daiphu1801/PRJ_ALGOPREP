package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import java.util.UUID;

/**
 * Internal caller authenticates/authorizes first. correlationId never enters generated artifacts.
 */
public record PrepareSubmissionCommand(
    UUID problemId,
    Language language,
    SubmissionModel model,
    String sourceCode,
    Integer expectedSpecVersion,
    String correlationId) {}
