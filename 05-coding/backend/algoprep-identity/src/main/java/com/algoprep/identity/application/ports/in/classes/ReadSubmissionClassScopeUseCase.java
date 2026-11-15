package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.domain.model.classes.SubmissionClassScope;
import java.util.List;
import java.util.UUID;

/** Trusted scope reader for F4 BR-JD-10; caller checks CLASS_MANAGEMENT:READ. */
public interface ReadSubmissionClassScopeUseCase {
  List<SubmissionClassScope> read(UUID instructorId);
}
