package com.algoprep.aireview.domain.model;

/** What a client should assume about the next AI reply (DD api F5 section 2.4). */
public enum ReplyState {
  IDLE,
  GENERATING,
  FAILED
}
