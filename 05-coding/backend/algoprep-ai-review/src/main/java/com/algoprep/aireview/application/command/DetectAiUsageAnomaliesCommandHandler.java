package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.AnomalyRunOutcome;
import com.algoprep.aireview.application.ports.in.DetectAiUsageAnomaliesUseCase;
import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.AnomalyPolicy;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JOB-AI-05: flags accounts at or above the threshold times the average of the last 24 hours. The
 * average counts every role; ADMIN is dropped from the candidates afterwards through the identity
 * port. An account with an OPEN alert gets no second one. The role lookup failing throws, so
 * nothing is written and the next hour retries. Never touches the budget lock or the account.
 */
@Service
@Transactional
class DetectAiUsageAnomaliesCommandHandler implements DetectAiUsageAnomaliesUseCase {
  private final AiUsageRepository usage;
  private final UserDirectoryPort users;
  private final ClockPort clock;
  private final BigDecimal threshold;

  DetectAiUsageAnomaliesCommandHandler(
      AiUsageRepository usage,
      UserDirectoryPort users,
      ClockPort clock,
      @Value("${algoprep.ai.anomaly.threshold-ratio:5}") BigDecimal threshold) {
    this.usage = usage;
    this.users = users;
    this.clock = clock;
    this.threshold = threshold;
  }

  @Override
  public AnomalyRunOutcome detect() {
    if (!usage.tryAnomalyLock()) {
      return new AnomalyRunOutcome(true, 0);
    }
    Instant end = clock.now();
    Instant start = end.minus(24, ChronoUnit.HOURS);
    List<AnomalyPolicy.Candidate> flagged =
        AnomalyPolicy.flagged(usage.callCountsSince(start), threshold);
    if (flagged.isEmpty()) {
      return new AnomalyRunOutcome(false, 0);
    }
    Set<UUID> open = usage.usersWithOpenAlert();
    List<AnomalyPolicy.Candidate> fresh =
        flagged.stream().filter(c -> !open.contains(c.userId())).toList();
    if (fresh.isEmpty()) {
      return new AnomalyRunOutcome(false, 0);
    }
    Map<UUID, CallerCategory> categories =
        users.getBaseCategories(fresh.stream().map(AnomalyPolicy.Candidate::userId).toList());
    int created = 0;
    for (AnomalyPolicy.Candidate c : fresh) {
      // An unknown role is not guessed: no alert until identity answers for the account.
      CallerCategory category = categories.get(c.userId());
      if (category == null || category == CallerCategory.ADMIN) {
        continue;
      }
      usage.insertAlert(
          new AnomalyAlert(
              UUID.randomUUID(),
              c.userId(),
              start,
              end,
              c.callCount(),
              c.baselineAvg(),
              c.ratio(),
              end));
      created++;
    }
    return new AnomalyRunOutcome(false, created);
  }
}
