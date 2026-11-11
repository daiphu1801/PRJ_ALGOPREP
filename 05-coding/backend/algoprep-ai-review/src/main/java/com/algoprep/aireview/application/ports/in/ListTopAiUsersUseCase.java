package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.TopUserItem;
import java.util.List;

/** Endpoint 30 (last 30 days). */
public interface ListTopAiUsersUseCase {
  List<TopUserItem> topUsers(int limit);
}
