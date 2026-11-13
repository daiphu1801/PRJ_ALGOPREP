package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import java.util.UUID;

/** Typed immutable specification and version from one F2 database snapshot. */
public record ProblemSpecView(
    UUID problemId,
    int specVersion,
    FunctionSpecDraft signature,
    String stdinFormatMd,
    String stdoutFormatMd,
    boolean functionWrapperSupported) {}
