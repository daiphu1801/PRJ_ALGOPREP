package com.algoprep.aireview.domain.model.interview;

/** What a client should assume about the next AI reply (DD api F5 section 2.4). */
public enum ReplyState {
  IDLE,
  GENERATING,
  FAILED
}
