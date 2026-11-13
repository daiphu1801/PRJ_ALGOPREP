package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.UsageAlertItem;
import java.util.List;

/** Endpoint 28. */
public interface ListAiUsageAlertsUseCase {
  List<UsageAlertItem> list();
}
