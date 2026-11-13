package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurnProgressionTest {
  private static final int CEILING = 6;

  private static TurnProgression decide(
      InterviewStage stage, boolean complete, int stageTurns, int turnCountAfter, Integer max) {
    return TurnProgression.decide(
        new TurnProgression.Facts(stage, complete, stageTurns, CEILING, turnCountAfter, max));
  }

  @Test
  void aNormalTurnKeepsTheStage() {
    TurnProgression p = decide(InterviewStage.EXPLAIN, false, 2, 2, 8);
    assertThat(p.nextStage()).isEqualTo(InterviewSessionStage.EXPLAIN);
    assertThat(p.aiLineStage()).isEqualTo(InterviewStage.EXPLAIN);
    assertThat(p.stageChanged()).isFalse();
    assertThat(p.shouldComplete()).isFalse();
  }

  @Test
  void stageCompleteMovesOnAndTheAiLineBelongsToTheNewStage() {
    TurnProgression p = decide(InterviewStage.EXPLAIN, true, 2, 2, 8);
    assertThat(p.nextStage()).isEqualTo(InterviewSessionStage.CHALLENGE);
    assertThat(p.aiLineStage()).isEqualTo(InterviewStage.CHALLENGE);
    assertThat(p.stageChanged()).isTrue();
    assertThat(p.forced()).isFalse();
    TurnProgression q = decide(InterviewStage.CHALLENGE, true, 1, 3, 8);
    assertThat(q.nextStage()).isEqualTo(InterviewSessionStage.SCALE_UP);
  }

  @Test
  void theCeilingForcesTheChangeAndTheAiLineStaysInTheOldStage() {
    TurnProgression p = decide(InterviewStage.EXPLAIN, false, CEILING, 6, null);
    assertThat(p.nextStage()).isEqualTo(InterviewSessionStage.CHALLENGE);
    assertThat(p.aiLineStage()).isEqualTo(InterviewStage.EXPLAIN);
    assertThat(p.stageChanged()).isTrue();
    assertThat(p.forced()).isTrue();
    assertThat(decide(InterviewStage.CHALLENGE, false, CEILING - 1, 9, null).stageChanged())
        .isFalse();
  }

  @Test
  void theLastStageNeverMovesAndCompletesOnTheFlagOrTheCeiling() {
    assertThat(decide(InterviewStage.SCALE_UP, false, 2, 10, null).shouldComplete()).isFalse();
    TurnProgression flagged = decide(InterviewStage.SCALE_UP, true, 2, 10, null);
    assertThat(flagged.shouldComplete()).isTrue();
    assertThat(flagged.nextStage()).isEqualTo(InterviewSessionStage.SCALE_UP);
    assertThat(flagged.stageChanged()).isFalse();
    assertThat(decide(InterviewStage.SCALE_UP, false, CEILING, 10, null).shouldComplete()).isTrue();
  }

  @Test
  void reachingMaxTurnsAlwaysAsksTheClientToComplete() {
    assertThat(decide(InterviewStage.EXPLAIN, false, 1, 8, 8).shouldComplete()).isTrue();
    assertThat(decide(InterviewStage.EXPLAIN, false, 1, 7, 8).shouldComplete()).isFalse();
    assertThat(decide(InterviewStage.CHALLENGE, true, 1, 8, 8).shouldComplete()).isTrue();
    assertThat(decide(InterviewStage.SCALE_UP, false, 1, 8, 8).shouldComplete()).isTrue();
  }
}
