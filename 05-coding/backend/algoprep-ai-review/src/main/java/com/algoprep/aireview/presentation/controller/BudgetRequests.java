package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiModelPriceNotFoundException;
import com.algoprep.aireview.domain.model.LimitsPatch;
import java.math.BigDecimal;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Shape checks of endpoints 19 and 22 to 25: AIR-V15 to AIR-V18 (DD validation F5 section 9). */
final class BudgetRequests {
  static final long MAX_LIMIT_TOKENS = 9_000_000_000_000L;
  static final int MAX_MODEL_NAME = 100;
  static final BigDecimal MAX_PRICE = new BigDecimal("999999.9999");
  private static final int MAX_PRICE_SCALE = 4;

  /** A price edit; a null component is absent from the body. */
  record PricePatch(String modelName, BigDecimal usdPerMillionTokens) {}

  private BudgetRequests() {}

  static long limitTokens(JsonNode body) {
    JsonNode node = body == null ? null : body.get("limitTokens");
    if (node == null
        || !node.isIntegralNumber()
        || !node.canConvertToLong()
        || node.asLong() < 1
        || node.asLong() > MAX_LIMIT_TOKENS) {
      throw new AiInvalidRequestException("AIR-V15", "limitTokens must be a positive integer");
    }
    return node.asLong();
  }

  /** At least one of the four fields; unknown fields are ignored. */
  static LimitsPatch limits(JsonNode body) {
    Integer review = intField(body, "reviewPerHour", 999);
    Integer interview = intField(body, "interviewPerHour", 999);
    Integer cooldown = intField(body, "cooldownSeconds", 999);
    Integer warn = intField(body, "warnPercent", 100);
    if (review == null && interview == null && cooldown == null && warn == null) {
      throw v16();
    }
    return new LimitsPatch(review, interview, cooldown, warn);
  }

  private static Integer intField(JsonNode body, String name, int max) {
    if (body == null || !body.has(name)) {
      return null;
    }
    JsonNode node = body.get(name);
    if (!node.isIntegralNumber()
        || !node.canConvertToInt()
        || node.asInt() < 1
        || node.asInt() > max) {
      throw v16();
    }
    return node.asInt();
  }

  /** Required name of the create endpoint. */
  static String createName(JsonNode body) {
    return modelName(body == null ? null : body.get("modelName"));
  }

  /** Edit body: each present field is validated; at least one must be present. */
  static PricePatch pricePatch(JsonNode body) {
    boolean hasName = body != null && body.has("modelName");
    boolean hasPrice = body != null && body.has("usdPerMillionTokens");
    if (!hasName && !hasPrice) {
      throw v17();
    }
    return new PricePatch(
        hasName ? modelName(body.get("modelName")) : null,
        hasPrice ? price(body.get("usdPerMillionTokens")) : null);
  }

  private static String modelName(JsonNode node) {
    if (node == null || !node.isString()) {
      throw v17();
    }
    String trimmed = node.asString().strip();
    if (trimmed.isEmpty() || trimmed.length() > MAX_MODEL_NAME) {
      throw v17();
    }
    return trimmed;
  }

  private static BigDecimal price(JsonNode node) {
    if (!node.isNumber()) {
      throw v18();
    }
    BigDecimal value = node.decimalValue();
    if (value.signum() < 0
        || value.stripTrailingZeros().scale() > MAX_PRICE_SCALE
        || value.compareTo(MAX_PRICE) > 0) {
      throw v18();
    }
    return value;
  }

  /** A path id that is not a UUID names no row, so it is the same 404 as an unknown id. */
  static UUID priceId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException notUuid) {
      throw new AiModelPriceNotFoundException();
    }
  }

  private static AiInvalidRequestException v16() {
    return new AiInvalidRequestException(
        "AIR-V16", "Provide at least one valid limit within its range");
  }

  private static AiInvalidRequestException v17() {
    return new AiInvalidRequestException(
        "AIR-V17", "modelName must be 1 to 100 characters after trimming");
  }

  private static AiInvalidRequestException v18() {
    return new AiInvalidRequestException(
        "AIR-V18", "usdPerMillionTokens must be 0 to 999999.9999 with at most 4 decimals");
  }
}
