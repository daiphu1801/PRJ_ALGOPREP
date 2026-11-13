package com.algoprep.identity.application.ports.out;

/** One-time {@code state} values of started OAuth flows (DD F1 BR-03, 10 minutes). */
public interface OAuthStatePort {

  void store(String state);

  /** True once for a stored, unexpired state; the value is gone afterwards. */
  boolean consume(String state);
}
