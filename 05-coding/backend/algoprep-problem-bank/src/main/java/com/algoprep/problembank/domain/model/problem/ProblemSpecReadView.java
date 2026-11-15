package com.algoprep.problembank.domain.model.problem;

import java.util.UUID;

/** One persisted row snapshot including the current wrapper flag, without testcase content. */
public record ProblemSpecReadView(
    UUID problemId, int specVersion, ProblemSpecData data, boolean functionWrapperSupported) {}
