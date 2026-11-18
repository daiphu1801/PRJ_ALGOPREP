package com.algoprep.harness.application.ports.out;

import com.algoprep.harness.application.dto.ProblemSpecView;
import java.util.Optional;
import java.util.UUID;

public interface ProblemSpecReaderPort {
  Optional<ProblemSpecView> findSpec(UUID problemId);
}
