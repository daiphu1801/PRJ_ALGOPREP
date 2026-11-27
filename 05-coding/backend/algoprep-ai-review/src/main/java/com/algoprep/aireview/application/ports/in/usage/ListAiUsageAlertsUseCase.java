package com.algoprep.aireview.application.ports.in.usage;

import com.algoprep.aireview.application.dto.usage.UsageAlertItem;
import java.util.List;

/** Endpoint 28. */
public interface ListAiUsageAlertsUseCase {
  List<UsageAlertItem> list();
}
