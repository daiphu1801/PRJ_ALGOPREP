package com.algoprep.aireview.domain.model.usage;

import com.algoprep.aireview.domain.model.FeatureCode;
import java.time.LocalDate;

/** Tokens of one feature on one UTC+7 day. */
public record DailyFeatureTokens(LocalDate day, FeatureCode feature, long tokens) {}
