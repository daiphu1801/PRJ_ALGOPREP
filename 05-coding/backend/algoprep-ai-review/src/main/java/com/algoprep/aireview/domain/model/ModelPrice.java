package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of {@code ai_model_prices}; the price is USD per one million tokens. */
public record ModelPrice(UUID id, String modelName, BigDecimal usdPerMillionTokens) {}
