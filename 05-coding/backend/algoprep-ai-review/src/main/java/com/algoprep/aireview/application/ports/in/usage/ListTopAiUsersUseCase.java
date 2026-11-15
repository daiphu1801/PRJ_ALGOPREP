package com.algoprep.aireview.application.ports.in.usage;

import com.algoprep.aireview.application.dto.usage.TopUserItem;
import java.util.List;

/** Endpoint 30 (last 30 days). */
public interface ListTopAiUsersUseCase {
  List<TopUserItem> topUsers(int limit);
}
