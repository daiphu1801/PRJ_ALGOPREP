package com.algoprep.problembank.application.dto;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import java.util.UUID;

public record ProblemSpecReadResult(
    UUID problemId,
    int specVersion,
    FunctionSpecDraft signature,
    String stdinFormatMd,
    String stdoutFormatMd,
    boolean functionWrapperSupported) {}
