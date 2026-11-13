package com.algoprep.aireview.domain.model;

/**
 * Stage decision of one finished turn (BR-AI-07 step 10), a pure function of the AI's {@code
 * stageComplete}, the number of USER lines in the stage (this one included), the stage ceiling and
 * the turn budget.
 *
 * @param nextStage stage stored on the session after the turn
 * @param aiLineStage {@code stage_at_turn} of the AI line: the NEW stage after an AI-declared
 *     change (the line is the opening of that stage), the OLD stage after a forced change
 * @param stageChanged the session moved to another stage
 * @param forced the change was forced by the ceiling, so the next turn tells the model so
 * @param shouldComplete the client should now finish the session
 */
public record TurnProgression(
    InterviewSessionStage nextStage,
    InterviewStage aiLineStage,
    boolean stageChanged,
    boolean forced,
    boolean shouldComplete) {

  /**
   * What the decision needs.
   *
   * @param userTurnsInStage USER lines of the current stage, the one just answered included
   * @param turnCountAfter {@code turn_count} once this turn is committed
   */
  public record Facts(
      InterviewStage stage,
      boolean stageComplete,
      int userTurnsInStage,
      int ceiling,
      int turnCountAfter,
      Integer maxTurns) {}

  public static TurnProgression decide(Facts facts) {
    InterviewStage stage = facts.stage();
    boolean stageComplete = facts.stageComplete();
    int userTurnsInStage = facts.userTurnsInStage();
    int ceiling = facts.ceiling();
    int turnCountAfter = facts.turnCountAfter();
    Integer maxTurns = facts.maxTurns();
    boolean outOfTurns = maxTurns != null && turnCountAfter >= maxTurns;
    if (stage == InterviewStage.SCALE_UP) {
      boolean complete = stageComplete || userTurnsInStage >= ceiling || outOfTurns;
      return new TurnProgression(InterviewSessionStage.SCALE_UP, stage, false, false, complete);
    }
    InterviewStage next =
        stage == InterviewStage.EXPLAIN ? InterviewStage.CHALLENGE : InterviewStage.SCALE_UP;
    InterviewSessionStage nextStage = InterviewSessionStage.valueOf(next.name());
    if (stageComplete) {
      return new TurnProgression(nextStage, next, true, false, outOfTurns);
    }
    if (userTurnsInStage >= ceiling) {
      return new TurnProgression(nextStage, stage, true, true, outOfTurns);
    }
    return new TurnProgression(
        InterviewSessionStage.valueOf(stage.name()), stage, false, false, outOfTurns);
  }
}
