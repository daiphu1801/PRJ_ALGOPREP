package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.grading.GradingReadSnapshot;
import com.algoprep.problembank.domain.model.grading.StoredSampleTestcase;
import com.algoprep.problembank.domain.model.snapshot.SnapshotMetadata;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradingReadRepository {
  Optional<GradingReadSnapshot> findGradingSpec(UUID problemId);

  List<StoredSampleTestcase> findSamples(UUID problemId);

  Optional<SnapshotMetadata> findSnapshot(UUID problemId, int version);
}
