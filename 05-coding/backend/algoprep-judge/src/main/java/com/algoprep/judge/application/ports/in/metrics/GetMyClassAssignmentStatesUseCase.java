package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import java.util.List;
import java.util.Map;

/** P7: F2 verifies current enrollment; identity is always the current STUDENT principal. */
public interface GetMyClassAssignmentStatesUseCase {
  Map<AssignmentPeriod.Key, AssignmentSolveState> read(List<AssignmentPeriod> assignments);
}
