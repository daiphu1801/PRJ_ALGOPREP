package com.algoprep.identity.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.ports.in.ListMyRecentActivityUseCase;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** At most 10 entries of the last 90 days, only those the caller is the actor of. */
@Service
class ListMyRecentActivityQueryHandler implements ListMyRecentActivityUseCase {
  static final int LIMIT = 10;
  static final Duration WINDOW = Duration.ofDays(90);

  private final MyProgressReadRepository progress;
  private final ClockPort clock;

  ListMyRecentActivityQueryHandler(MyProgressReadRepository progress, ClockPort clock) {
    this.progress = progress;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ActivityEntry> list(UUID userId) {
    return progress.recentActivity(userId, clock.now().minus(WINDOW), LIMIT);
  }
}
