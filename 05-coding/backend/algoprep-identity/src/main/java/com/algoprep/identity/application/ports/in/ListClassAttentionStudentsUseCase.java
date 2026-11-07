package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassMetricViews.AttentionStudent;
import java.util.List;
import java.util.UUID;

/** Up to five students who need attention, most serious first (#58). */
public interface ListClassAttentionStudentsUseCase {
  List<AttentionStudent> list(UUID instructorId, UUID classId);
}
