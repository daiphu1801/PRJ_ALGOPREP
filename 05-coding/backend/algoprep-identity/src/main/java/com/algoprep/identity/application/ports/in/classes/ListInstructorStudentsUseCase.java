package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.application.dto.classes.InstructorStudentsQuery;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.StudentPage;

/** One page of (class, student) rows across the classes of the instructor (#59). */
public interface ListInstructorStudentsUseCase {
  StudentPage list(InstructorStudentsQuery query);
}
