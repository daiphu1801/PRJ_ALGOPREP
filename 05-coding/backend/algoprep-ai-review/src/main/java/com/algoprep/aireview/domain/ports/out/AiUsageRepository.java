package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.AlertReview;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.DailyFeatureTokens;
import com.algoprep.aireview.domain.model.ProblemUsage;
import com.algoprep.aireview.domain.model.UserUsage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Reads of {@code token_usage} for the admin usage endpoints and the anomaly job (F5-4). */
public interface AiUsageRepository {
  /** Tokens per UTC+7 day and feature in {@code [from, to)}; empty days are absent. */
  List<DailyFeatureTokens> dailyTokens(Instant from, Instant to);

  /** Highest token total first, then user id; cost joins prices on lower(model_name). */
  List<UserUsage> topUsers(Instant since, int limit);

  /** Highest token total first, then problem id; rows without a problem are left out. */
  List<ProblemUsage> topProblems(Instant since, int limit);

  /** OPEN alerts, newest first, bounded. */
  List<AnomalyAlert> openAlerts();

  /** Empty when the alert does not exist; otherwise the stored review state. */
  Optional<AlertReview> markReviewed(UUID alertId);

  /** Transaction-scoped try-lock so two instances do not run JOB-AI-05 together. */
  boolean tryAnomalyLock();

  /** Number of {@code token_usage} rows per user since the instant. */
  Map<UUID, Integer> callCountsSince(Instant since);

  Set<UUID> usersWithOpenAlert();

  void insertAlert(AnomalyAlert alert);
}
