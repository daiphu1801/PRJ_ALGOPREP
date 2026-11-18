package com.algoprep.harness.application.dto;

/** P4 exposes only learner output; the caller controls Sample versus Hidden visibility. */
public record MatchResult(MatchVerdict verdict, String actualOutput, String userConsole) {}
