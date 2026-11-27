package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;

public record StarterCodeResult(
    Language language, SubmissionModel submissionModel, String sourceCode, int specVersion) {}
