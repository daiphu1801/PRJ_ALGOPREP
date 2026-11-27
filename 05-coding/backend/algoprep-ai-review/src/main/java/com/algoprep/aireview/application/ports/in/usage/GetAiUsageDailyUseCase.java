package com.algoprep.aireview.application.ports.in.usage;

import com.algoprep.aireview.application.dto.usage.UsageDaily;

/** Endpoint 27. */
public interface GetAiUsageDailyUseCase {
  UsageDaily daily(int rangeDays);
}
