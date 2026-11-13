package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ProgressCounters;
import com.algoprep.identity.domain.model.SolveState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read side of the personal-progress models (#46, #47, #48, #55); every read is by the caller. */
public interface MyProgressReadRepository {

  /** Stored counters; all zero and no language for a user with no graded submission. */
  ProgressCounters counters(UUID userId);

  /**
   * Newest first (ties by id), only entries the user is the actor of, at or after {@code since}.
   */
  List<ActivityEntry> recentActivity(UUID userId, Instant since, int limit);

  /** Days that have a row between {@code from} and {@code to} inclusive; other days are absent. */
  Map<LocalDate, Integer> dailyCounts(UUID userId, LocalDate from, LocalDate to);

  /** State of the given problems only; problems with no best-score row are absent. */
  Map<UUID, SolveState> solveStates(UUID userId, Collection<UUID> problemIds);

  /** Every problem the user has a best verdict for. */
  Map<UUID, SolveState> allSolveStates(UUID userId);
}
