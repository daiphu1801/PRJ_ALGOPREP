package com.algoprep.harness.application.ports.out;

import java.util.UUID;

public interface StarterCodeAccessPort {
  boolean canRead(UUID problemId, UUID callerUserId, String baseCategory);
}
