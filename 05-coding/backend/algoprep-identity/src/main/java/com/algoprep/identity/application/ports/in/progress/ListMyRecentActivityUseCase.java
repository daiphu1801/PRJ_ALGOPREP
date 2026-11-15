package com.algoprep.identity.application.ports.in.progress;

import com.algoprep.identity.domain.model.progress.ActivityEntry;
import java.util.List;
import java.util.UUID;

/** Personal progress read #46 (10 entries, 90 days). */
public interface ListMyRecentActivityUseCase {
  List<ActivityEntry> list(UUID userId);
}
