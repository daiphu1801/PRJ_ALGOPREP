package com.algoprep.problembank.application.ports.in;

import java.util.UUID;

/** Identity/category must come from a trusted authentication adapter, never request data. */
public interface StarterCodeAccessQuery {
  boolean canRead(UUID problemId, UUID callerUserId, String baseCategory);
}
