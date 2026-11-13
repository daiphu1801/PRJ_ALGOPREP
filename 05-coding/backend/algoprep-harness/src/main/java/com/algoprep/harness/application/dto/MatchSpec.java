package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeNode;
import java.math.BigDecimal;

/** Immutable matching configuration captured from the same specification as the program. */
public record MatchSpec(
    MatchingStrategy matchingStrategy, BigDecimal epsilonValue, TypeNode returnType) {}
