package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiUsageAlertNotFoundException;
import java.util.Set;
import java.util.UUID;

/** Query and path checks of endpoints 27 to 31: AIR-V19, AIR-V02 and the AIR-120 id rule. */
final class UsageRequests {
  static final int DEFAULT_RANGE_DAYS = 14;
  static final int DEFAULT_LIMIT = 5;
  static final int MAX_LIMIT = 20;
  private static final Set<Integer> RANGES = Set.of(7, 14, 30);

  private UsageRequests() {}

  /** One of 7, 14, 30; absent means 14. */
  static int rangeDays(String raw) {
    if (raw == null) {
      return DEFAULT_RANGE_DAYS;
    }
    try {
      int value = Integer.parseInt(raw.strip());
      if (RANGES.contains(value)) {
        return value;
      }
    } catch (NumberFormatException notNumber) {
      // falls through to the same validation error
    }
    throw new AiInvalidRequestException("AIR-V19", "rangeDays must be 7, 14 or 30");
  }

  /** 1 to 20; absent means 5. */
  static int limit(String raw) {
    if (raw == null) {
      return DEFAULT_LIMIT;
    }
    try {
      int value = Integer.parseInt(raw.strip());
      if (value >= 1 && value <= MAX_LIMIT) {
        return value;
      }
    } catch (NumberFormatException notNumber) {
      // falls through to the same validation error
    }
    throw new AiInvalidRequestException("AIR-V02", "limit must be 1 to 20");
  }

  /** A path id that is not a UUID names no alert, so it is the same 404 as an unknown id. */
  static UUID alertId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException notUuid) {
      throw new AiUsageAlertNotFoundException();
    }
  }
}
