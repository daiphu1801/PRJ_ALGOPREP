package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.InstructorReadFilter;
import com.algoprep.judge.domain.model.ScopedSubmission;
import java.util.List;

/** All membership and assignment intervals are applied in SQL before pagination. */
public interface InstructorSubmissionRepository {
  long count(InstructorReadFilter filter);

  List<ScopedSubmission> list(InstructorReadFilter filter, int offset, int limit);
}
