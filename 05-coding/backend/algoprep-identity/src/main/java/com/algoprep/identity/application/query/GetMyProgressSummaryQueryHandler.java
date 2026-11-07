package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetMyProgressSummaryUseCase;
import com.algoprep.identity.domain.model.MyProgressSummary;
import com.algoprep.identity.domain.model.ProgressCounters;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rate divides by the eligible count (final status other than {@code SYSTEM_ERROR}), so an
 * infrastructure failure never lowers it while {@code totalSubmissions} still counts it.
 */
@Service
class GetMyProgressSummaryQueryHandler implements GetMyProgressSummaryUseCase {
  private final MyProgressReadRepository progress;

  GetMyProgressSummaryQueryHandler(MyProgressReadRepository progress) {
    this.progress = progress;
  }

  @Override
  @Transactional(readOnly = true)
  public MyProgressSummary get(UUID userId) {
    ProgressCounters c = progress.counters(userId);
    Integer rate =
        c.acEligibleCount() == 0
            ? null
            : (int) Math.round(c.acceptedCount() * 100.0 / c.acEligibleCount());
    return new MyProgressSummary(
        c.solvedProblemCount(),
        c.totalSubmissions(),
        c.acceptedCount(),
        rate,
        c.firstTryAcceptedCount(),
        c.topLanguage());
  }
}
