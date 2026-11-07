package com.algoprep.problembank.application.policy;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ranking of DD F2 section 13.5: problems the caller has not solved first; inside each group
 * the problem whose weakest topic has the lowest score first; ties by code. A topic without a score
 * counts as 100, so it never pulls a problem forward.
 */
public final class SuggestionRanking {
  public static final int NO_SCORE = 100;
  private static final double AC_WEIGHT = 0.7;
  private static final double SOLVED_WEIGHT = 0.3;

  private SuggestionRanking() {}

  /** {@code 100 * (0.7 * acRate + 0.3 * solved / total)}, rounded and kept within 0 to 100. */
  public static int score(double acRate, int solved, int total) {
    double raw = 100 * (AC_WEIGHT * acRate + SOLVED_WEIGHT * solved / total);
    return Math.clamp(Math.round(raw), 0, 100);
  }

  /**
   * {@code states} null means identity could not answer: nothing is known to be solved, so the
   * order is by score and code only. An empty {@code scores} map ranks by solve state and code.
   */
  public static List<Candidate> rank(
      List<Candidate> candidates, SolveStates states, Map<UUID, Integer> scores) {
    return candidates.stream()
        .sorted(
            Comparator.comparingInt((Candidate c) -> solved(states, c) ? 1 : 0)
                .thenComparingInt(c -> weakest(c, scores))
                .thenComparing(Candidate::code))
        .toList();
  }

  private static boolean solved(SolveStates states, Candidate candidate) {
    return states != null && states.solved().contains(candidate.id());
  }

  private static int weakest(Candidate candidate, Map<UUID, Integer> scores) {
    return candidate.topicIds().stream()
        .mapToInt(id -> scores.getOrDefault(id, NO_SCORE))
        .min()
        .orElse(NO_SCORE);
  }
}
