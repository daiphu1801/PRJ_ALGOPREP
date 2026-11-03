package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Trusted internal reads. No fallback from pinned snapshots to current testcase contents. */
public interface ProblemGradingSpecPort {
  Optional<GradingSpecification> loadGradingSpec(UUID problemId);

  List<GradingTestcase> loadSampleTestcases(UUID problemId);

  GradingTestcaseSnapshot loadTestcaseSnapshot(UUID problemId, int version);
}
