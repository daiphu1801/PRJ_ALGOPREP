package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.UsageDaily;

/** Endpoint 27. */
public interface GetAiUsageDailyUseCase {
  UsageDaily daily(int rangeDays);
}
