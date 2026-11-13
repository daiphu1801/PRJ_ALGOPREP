package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.SubmissionSnapshotLookup;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionSnapshotRepository {
  Optional<SubmissionSnapshotLookup> find(UUID submissionId);
}
