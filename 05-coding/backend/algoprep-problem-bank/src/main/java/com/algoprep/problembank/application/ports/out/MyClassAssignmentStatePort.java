package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * The caller's state per assignment interval, from the judge module (F4 P7). The caller is the
 * current student principal: no user id travels through this port.
 */
public interface MyClassAssignmentStatePort {
  /** Empty when the judge module cannot answer; callers must not guess "unsolved". */
  Optional<Map<AssignmentKey, ClassSolveState>> read(Collection<AssignmentKey> assignments);
}
