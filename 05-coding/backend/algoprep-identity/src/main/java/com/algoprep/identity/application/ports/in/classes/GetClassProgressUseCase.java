package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassMetricViews.ClassProgress;
import java.util.UUID;

/** Completion and the five labels of one owned class (#29). */
public interface GetClassProgressUseCase {
  ClassProgress get(UUID instructorId, UUID classId);
}
