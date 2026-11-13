package com.algoprep.aireview.domain.model;

import java.time.LocalDate;

/** Tokens of one feature on one UTC+7 day. */
public record DailyFeatureTokens(LocalDate day, FeatureCode feature, long tokens) {}
