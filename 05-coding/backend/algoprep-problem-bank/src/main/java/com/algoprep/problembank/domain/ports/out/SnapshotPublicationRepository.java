package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.SnapshotPublicationState;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnapshotPublicationRepository {
  Optional<SnapshotPublicationState> lockProblem(UUID problemId);

  List<StoredGradingTestcase> findApproved(UUID problemId);

  void insertSnapshot(SnapshotMetadata metadata);

  Instant bumpVersion(UUID problemId, int expectedVersion, int nextVersion);

  void appendBumpEvent(UUID problemId, String envelope);
}
