package com.algoprep.aireview.domain.model;

/** Result of the cooldown and hourly-quota reservation (BR-AI-04 steps 3 and 4). */
public enum GateOutcome {
  /** Cooldown set and quota counted; the caller refunds if the LLM call fails. */
  GRANTED,
  /** Still inside the cooldown, nothing was counted (AIR-404). */
  COOLDOWN,
  /** Hourly limit exceeded, the reservation was already undone inside the script (AIR-402/403). */
  QUOTA_EXCEEDED,
  /** Redis was unavailable: both checks count as passed (fail-open), nothing to refund. */
  BYPASSED
}
