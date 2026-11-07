package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.problembank.application.dto.SuggestedProblemViews.Item;
import com.algoprep.problembank.application.dto.SuggestedProblemViews.SuggestedProblems;
import com.algoprep.problembank.application.dto.SuggestedProblemViews.TopicScoreItem;
import com.algoprep.problembank.application.policy.SuggestionRanking;
import com.algoprep.problembank.application.ports.in.SuggestedProblemsUseCase;
import com.algoprep.problembank.application.ports.out.MyTopicSubmissionMetricsPort;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.TopicMetric;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.TopicScore;
import com.algoprep.problembank.domain.ports.out.HistoricalTopicProblemRepository;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import com.algoprep.problembank.domain.ports.out.SuggestedProblemReadRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 section 13. A gate that cannot answer never produces made-up numbers: without the solve
 * state the rows carry a null state, without the topic metrics the scores are left out, and either
 * way {@code rankingAvailable} is false while the problems are still returned.
 */
@Service
class SuggestedProblemsQueryHandler implements SuggestedProblemsUseCase {
  static final int LIMIT = 7;
  static final int BATCH = 50;

  private final SuggestedProblemReadRepository problems;
  private final StudentCatalogReadRepository catalog;
  private final HistoricalTopicProblemRepository history;
  private final SolveStatePort solveStates;
  private final MyTopicSubmissionMetricsPort metrics;

  SuggestedProblemsQueryHandler(
      SuggestedProblemReadRepository problems,
      StudentCatalogReadRepository catalog,
      HistoricalTopicProblemRepository history,
      SolveStatePort solveStates,
      MyTopicSubmissionMetricsPort metrics) {
    this.problems = problems;
    this.catalog = catalog;
    this.history = history;
    this.solveStates = solveStates;
    this.metrics = metrics;
  }

  @Override
  @Transactional(readOnly = true)
  public SuggestedProblems suggest(UUID userId, Map<String, String> params) {
    UUID topic = topic(params.get("topic"));
    String difficulty = params.get("difficulty");
    var candidates = problems.candidates(blank(difficulty) ? null : difficulty, topic);
    var states = solveStates.mine(userId).orElse(null);
    var scores = states == null ? Optional.<List<TopicScore>>empty() : scores(states);
    Map<UUID, Integer> byTopic =
        scores.orElse(List.of()).stream()
            .collect(Collectors.toMap(TopicScore::topicId, TopicScore::score));
    var items =
        SuggestionRanking.rank(candidates, states, byTopic).stream()
            .limit(LIMIT)
            .map(c -> item(c, states))
            .toList();
    var wire =
        scores.orElse(List.of()).stream()
            .map(s -> new TopicScoreItem(s.topicId(), s.topicName(), s.sortOrder(), s.score()))
            .toList();
    return new SuggestedProblems(items, wire, scores.isPresent());
  }

  /** Empty when any step of the vector fails; one failed batch spoils the whole vector. */
  private Optional<List<TopicScore>> scores(SolveStates states) {
    var topics =
        catalog.topicProgress(states).stream()
            .filter(t -> t.total() > 0)
            .sorted(
                Comparator.comparingInt(TopicProgress::sortOrder)
                    .thenComparing(t -> t.topicId().toString()))
            .toList();
    var read = read(topics);
    if (read.isEmpty()) {
      return Optional.empty();
    }
    var scores = new ArrayList<TopicScore>();
    for (var t : topics) {
      var m = read.get().get(t.topicId());
      if (m == null) {
        return Optional.empty();
      }
      if (m.submissionCount() > 0 && m.acRate() != null) {
        scores.add(
            new TopicScore(
                t.topicId(),
                t.name(),
                t.sortOrder(),
                SuggestionRanking.score(m.acRate(), t.solved(), t.total())));
      }
    }
    return Optional.of(scores);
  }

  private Optional<Map<UUID, TopicMetric>> read(List<TopicProgress> topics) {
    var all = new HashMap<UUID, TopicMetric>();
    if (topics.isEmpty()) {
      return Optional.of(all);
    }
    var ids = topics.stream().map(TopicProgress::topicId).collect(Collectors.toSet());
    var problemsOf = history.read(ids);
    for (int from = 0; from < topics.size(); from += BATCH) {
      var batch = new LinkedHashMap<UUID, Set<UUID>>();
      topics
          .subList(from, Math.min(topics.size(), from + BATCH))
          .forEach(t -> batch.put(t.topicId(), problemsOf.getOrDefault(t.topicId(), Set.of())));
      var part = metrics.read(batch);
      if (part.isEmpty()) {
        return Optional.empty();
      }
      all.putAll(part.get());
    }
    return Optional.of(all);
  }

  private static Item item(Candidate c, SolveStates states) {
    return new Item(
        c.id(),
        c.code(),
        c.title(),
        c.levelCode(),
        c.levelDisplayName(),
        c.topicNames().isEmpty() ? null : c.topicNames().get(0),
        c.acRate(),
        states == null ? null : stateOf(states, c.id()),
        c.functionWrapperSupported());
  }

  private static String stateOf(SolveStates states, UUID id) {
    if (states.solved().contains(id)) {
      return "SOLVED";
    }
    return states.attempted().contains(id) ? "ATTEMPTED" : "UNSOLVED";
  }

  private static UUID topic(String raw) {
    if (blank(raw)) {
      return null;
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("PRB-001", "topic must be a UUID");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
