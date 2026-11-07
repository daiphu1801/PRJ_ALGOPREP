package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.problembank.application.ports.out.MyTopicSubmissionMetricsPort;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.TopicMetric;
import com.algoprep.problembank.domain.ports.out.HistoricalTopicProblemRepository;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import com.algoprep.problembank.domain.ports.out.SuggestedProblemReadRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SuggestedProblemsQueryHandlerTest {
  private final SuggestedProblemReadRepository problems =
      mock(SuggestedProblemReadRepository.class);
  private final StudentCatalogReadRepository catalog = mock(StudentCatalogReadRepository.class);
  private final HistoricalTopicProblemRepository history =
      mock(HistoricalTopicProblemRepository.class);
  private final SolveStatePort solveStates = mock(SolveStatePort.class);
  private final MyTopicSubmissionMetricsPort metrics = mock(MyTopicSubmissionMetricsPort.class);
  private final SuggestedProblemsQueryHandler handler =
      new SuggestedProblemsQueryHandler(problems, catalog, history, solveStates, metrics);
  private final UUID user = UUID.randomUUID();
  private final UUID weak = UUID.randomUUID();
  private final UUID strong = UUID.randomUUID();
  private final UUID empty = UUID.randomUUID();

  @Test
  void aWrongTopicIsAValidationErrorBeforeAnyPortIsCalled() {
    assertThatThrownBy(() -> handler.suggest(user, Map.of("topic", "not-a-uuid")))
        .isInstanceOf(InvalidRequestException.class);

    verifyNoInteractions(problems, solveStates, metrics);
  }

  @Test
  void rankedByTheWeakestTopicWithTheVectorReturnedOnceAndNoScoreForTopicsWithoutSubmissions() {
    var weakProblem = candidate("b", weak);
    var strongProblem = candidate("a", strong);
    var solvedProblem = candidate("c", weak);
    when(problems.candidates(isNull(), isNull()))
        .thenReturn(List.of(solvedProblem, strongProblem, weakProblem));
    when(solveStates.mine(user))
        .thenReturn(Optional.of(new SolveStates(Set.of(solvedProblem.id()), Set.of())));
    when(catalog.topicProgress(any()))
        .thenReturn(
            List.of(
                new TopicProgress(strong, "Strong", 2, 4, 4),
                new TopicProgress(weak, "Weak", 1, 4, 1),
                new TopicProgress(empty, "Empty", 3, 4, 0),
                new TopicProgress(UUID.randomUUID(), "NoProblems", 4, 0, 0)));
    when(history.read(any())).thenReturn(Map.of(weak, Set.of(weakProblem.id())));
    when(metrics.read(anyMap()))
        .thenReturn(
            Optional.of(
                Map.of(
                    weak, new TopicMetric(4, 1, 0.25),
                    strong, new TopicMetric(5, 5, 1.0),
                    empty, new TopicMetric(0, 0, null))));

    var result = handler.suggest(user, Map.of());

    assertThat(result.rankingAvailable()).isTrue();
    assertThat(result.items()).extracting("code").containsExactly("b", "a", "c");
    assertThat(result.items().get(2).solveState()).isEqualTo("SOLVED");
    assertThat(result.items().get(0).solveState()).isEqualTo("UNSOLVED");
    assertThat(result.topicScores()).extracting("topicId").containsExactly(weak, strong);
    assertThat(result.topicScores()).extracting("score").containsExactly(25, 100);
    verify(metrics, times(1)).read(anyMap());
  }

  @Test
  void aFailingJudgeGateKeepsTheProblemsButDropsTheScores() {
    var one = candidate("b", weak);
    var two = candidate("a", weak);
    when(problems.candidates(isNull(), isNull())).thenReturn(List.of(one, two));
    when(solveStates.mine(user))
        .thenReturn(Optional.of(new SolveStates(Set.of(), Set.of(one.id()))));
    when(catalog.topicProgress(any()))
        .thenReturn(List.of(new TopicProgress(weak, "Weak", 1, 2, 0)));
    when(history.read(any())).thenReturn(Map.of());
    when(metrics.read(anyMap())).thenReturn(Optional.empty());

    var result = handler.suggest(user, Map.of());

    assertThat(result.rankingAvailable()).isFalse();
    assertThat(result.topicScores()).isEmpty();
    assertThat(result.items()).extracting("code").containsExactly("a", "b");
    assertThat(result.items().get(1).solveState()).isEqualTo("ATTEMPTED");
  }

  @Test
  void aJudgeAnswerMissingATopicIsTreatedAsAFailedGate() {
    when(problems.candidates(isNull(), isNull())).thenReturn(List.of(candidate("a", weak)));
    when(solveStates.mine(user)).thenReturn(Optional.of(new SolveStates(Set.of(), Set.of())));
    when(catalog.topicProgress(any()))
        .thenReturn(List.of(new TopicProgress(weak, "Weak", 1, 2, 0)));
    when(history.read(any())).thenReturn(Map.of());
    when(metrics.read(anyMap())).thenReturn(Optional.of(Map.of()));

    var result = handler.suggest(user, Map.of());

    assertThat(result.rankingAvailable()).isFalse();
    assertThat(result.topicScores()).isEmpty();
  }

  @Test
  void withoutIdentityTheRowsHaveNoStateAreOrderedByCodeAndTheJudgeIsNotAsked() {
    when(problems.candidates(isNull(), isNull()))
        .thenReturn(List.of(candidate("b", weak), candidate("a", weak)));
    when(solveStates.mine(user)).thenReturn(Optional.empty());

    var result = handler.suggest(user, Map.of());

    assertThat(result.rankingAvailable()).isFalse();
    assertThat(result.topicScores()).isEmpty();
    assertThat(result.items()).extracting("code").containsExactly("a", "b");
    assertThat(result.items()).extracting("solveState").containsOnlyNulls();
    verify(metrics, never()).read(anyMap());
    verify(catalog, never()).topicProgress(any());
  }

  @Test
  void anEmptyVectorIsAvailableButHasNoScores() {
    when(problems.candidates(isNull(), isNull())).thenReturn(List.of(candidate("a", weak)));
    when(solveStates.mine(user)).thenReturn(Optional.of(new SolveStates(Set.of(), Set.of())));
    when(catalog.topicProgress(any()))
        .thenReturn(List.of(new TopicProgress(weak, "Weak", 1, 0, 0)));

    var result = handler.suggest(user, Map.of());

    assertThat(result.rankingAvailable()).isTrue();
    assertThat(result.topicScores()).isEmpty();
    verifyNoInteractions(metrics);
  }

  @Test
  void onlySevenRowsAreReturnedAndMoreThanFiftyTopicsAreSentInBatches() {
    var rows = new ArrayList<Candidate>();
    IntStream.range(0, 9).forEach(i -> rows.add(candidate("p" + i, weak)));
    when(problems.candidates(isNull(), isNull())).thenReturn(rows);
    when(solveStates.mine(user)).thenReturn(Optional.of(new SolveStates(Set.of(), Set.of())));
    var topics = new ArrayList<TopicProgress>();
    IntStream.range(0, 120)
        .forEach(i -> topics.add(new TopicProgress(UUID.randomUUID(), "T" + i, i, 2, 1)));
    when(catalog.topicProgress(any())).thenReturn(topics);
    when(history.read(any())).thenReturn(Map.of());
    when(metrics.read(anyMap()))
        .thenAnswer(
            call -> {
              Map<UUID, Set<UUID>> batch = call.getArgument(0);
              assertThat(batch.size()).isLessThanOrEqualTo(50);
              var answer = new HashMap<UUID, TopicMetric>();
              batch.keySet().forEach(id -> answer.put(id, new TopicMetric(2, 1, 0.5)));
              return Optional.of(answer);
            });

    var result = handler.suggest(user, Map.of());

    assertThat(result.items()).hasSize(7);
    assertThat(result.topicScores()).hasSize(120);
    verify(metrics, times(3)).read(anyMap());
  }

  @Test
  void theDifficultyAndTopicFiltersReachTheRepositoryAndKeepTheVectorUnfiltered() {
    when(problems.candidates("easy", weak)).thenReturn(List.of());
    when(solveStates.mine(user)).thenReturn(Optional.empty());

    var result = handler.suggest(user, Map.of("difficulty", "easy", "topic", weak.toString()));

    assertThat(result.items()).isEmpty();
    verify(problems).candidates("easy", weak);
  }

  private static Candidate candidate(String code, UUID topic) {
    return new Candidate(
        UUID.randomUUID(),
        code,
        "Title " + code,
        "easy",
        "Easy",
        List.of("Topic"),
        List.of(topic),
        null,
        true);
  }
}
