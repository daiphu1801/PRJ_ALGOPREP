package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.InstructorStudentsQuery;
import com.algoprep.identity.domain.model.ClassMetricViews.StudentPage;

/** One page of (class, student) rows across the classes of the instructor (#59). */
public interface ListInstructorStudentsUseCase {
  StudentPage list(InstructorStudentsQuery query);
}
