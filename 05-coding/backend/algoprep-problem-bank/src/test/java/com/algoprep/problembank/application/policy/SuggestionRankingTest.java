package com.algoprep.problembank.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuggestionRankingTest {
  private static final UUID WEAK = UUID.randomUUID();
  private static final UUID STRONG = UUID.randomUUID();

  @Test
  void scoreBlendsAcRateAndSolvedShareAndStaysInRange() {
    assertThat(SuggestionRanking.score(0.5, 2, 4)).isEqualTo(50);
    assertThat(SuggestionRanking.score(0, 0, 3)).isZero();
    assertThat(SuggestionRanking.score(1, 3, 3)).isEqualTo(100);
    assertThat(SuggestionRanking.score(1.5, 3, 3)).isEqualTo(100);
  }

  @Test
  void unsolvedComeFirstThenTheWeakestTopicThenTheCode() {
    var weakUnsolved = problem("c", WEAK);
    var strongUnsolved = problem("a", STRONG);
    var solvedWeak = problem("b", WEAK);
    var noTopic = problem("d");
    var states = new SolveStates(Set.of(solvedWeak.id()), Set.of(weakUnsolved.id()));

    var ranked =
        SuggestionRanking.rank(
            List.of(solvedWeak, noTopic, strongUnsolved, weakUnsolved),
            states,
            Map.of(WEAK, 20, STRONG, 90));

    assertThat(ranked).extracting(Candidate::code).containsExactly("c", "a", "d", "b");
  }

  @Test
  void aProblemTakesItsLowestTopicScoreAndAnUnscoredTopicCountsAsOneHundred() {
    var both = problem("z", STRONG, WEAK);
    var other = problem("a", STRONG);

    var ranked =
        SuggestionRanking.rank(
            List.of(other, both), new SolveStates(Set.of(), Set.of()), Map.of(WEAK, 10));

    assertThat(ranked).extracting(Candidate::code).containsExactly("z", "a");
  }

  @Test
  void withoutSolveStateNothingCountsAsSolvedAndTheOrderIsByScoreThenCode() {
    var first = problem("b", WEAK);
    var second = problem("a", STRONG);

    var ranked = SuggestionRanking.rank(List.of(second, first), null, Map.of(WEAK, 5));

    assertThat(ranked).extracting(Candidate::code).containsExactly("b", "a");
  }

  private static Candidate problem(String code, UUID... topics) {
    return new Candidate(
        UUID.randomUUID(),
        code,
        "Title " + code,
        "easy",
        "Easy",
        List.of(),
        List.of(topics),
        null,
        true);
  }
}
