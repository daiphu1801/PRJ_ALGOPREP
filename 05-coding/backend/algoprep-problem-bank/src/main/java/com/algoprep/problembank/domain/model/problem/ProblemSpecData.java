package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;

/** The eight persisted columns of a problem specification; raw type JSON stays lossless. */
public record ProblemSpecData(
    String matchingStrategy,
    BigDecimal epsilonValue,
    String stdinFormatMd,
    String stdoutFormatMd,
    String functionName,
    String returnTypeJson,
    String parametersJson,
    String nameOverridesJson) {}
