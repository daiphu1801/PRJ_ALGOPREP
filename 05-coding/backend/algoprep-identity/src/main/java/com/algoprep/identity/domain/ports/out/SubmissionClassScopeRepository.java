package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.SubmissionClassScope;
import java.util.List;
import java.util.UUID;

public interface SubmissionClassScopeRepository {
  List<SubmissionClassScope> read(UUID instructorId);
}
