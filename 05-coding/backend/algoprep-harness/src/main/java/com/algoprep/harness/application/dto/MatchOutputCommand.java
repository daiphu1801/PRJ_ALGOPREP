package com.algoprep.harness.application.dto;

/** Inputs to P4; expected output must never be included in diagnostics or the result. */
public record MatchOutputCommand(
    SubmissionModel model,
    MatchSpec matchSpec,
    String resultSentinel,
    String actualStdout,
    String expectedOutput) {}
