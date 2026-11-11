package com.algoprep.problembank.application.ports.out;

import java.util.UUID;

/** {@code problem:gen:lock:<problemId>}, 10 minutes: one generation per problem at a time. */
public interface TestcaseGenerationLockPort {
  /** Token of the held lock, or null when another run holds it. Redis down means a token anyway. */
  String acquire(UUID problemId);

  /** Releases only a lock still owned by {@code token}; never throws. */
  void release(UUID problemId, String token);
}
