package com.algoprep.identity.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.application.ports.out.ClassMetricsPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.model.ClassProgressRules;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.StudentMetrics;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Joins the class tables of identity with the assignments of problem-bank and the figures of the
 * judge, once, for every class metric read (#26, #28, #29, #56 to #59). Any failure of the two
 * outside sources is one {@code 503 IDT-507} without detail, never a row of zeros.
 */
@Service
class ClassMetricsAssembler {
  /** One owned class with its students and their figures. */
  record ClassRows(InstructorClass owned, List<ClassStudentRow> rows) {}

  private final InstructorClassRepository classes;
  private final ClassAssignmentLookupPort assignments;
  private final ClassMetricsPort metrics;
  private final ClockPort clock;

  ClassMetricsAssembler(
      InstructorClassRepository classes,
      ClassAssignmentLookupPort assignments,
      ClassMetricsPort metrics,
      ClockPort clock) {
    this.classes = classes;
    this.assignments = assignments;
    this.metrics = metrics;
    this.clock = clock;
  }

  Instant now() {
    return clock.now();
  }

  static ResourceNotFoundException classNotFound() {
    return new ResourceNotFoundException(
        IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available");
  }

  static ServiceUnavailableException unavailable() {
    return new ServiceUnavailableException(
        IdentityErrorCodes.CLASS_METRICS_UNAVAILABLE,
        "The class figures are temporarily unavailable",
        null);
  }

  /** The class of the caller, or every class of the caller when {@code classId} is null. */
  List<InstructorClass> scope(UUID instructorId, UUID classId) {
    if (classId == null) {
      return classes.listOwned(instructorId);
    }
    return List.of(classes.findOwned(classId, instructorId).orElseThrow(() -> classNotFound()));
  }

  /** Every assignment interval of the classes, grouped by class; one call to problem-bank. */
  Map<UUID, List<ClassProblemWindow>> windows(Collection<UUID> classIds) {
    try {
      return assignments.assignmentsOf(classIds).stream()
          .collect(Collectors.groupingBy(ClassProblemWindow::classId));
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  /** Every active student of every class in the scope, with figures. */
  List<ClassRows> everyStudent(List<InstructorClass> scope) {
    Map<UUID, List<ClassProblemWindow>> windows =
        windows(scope.stream().map(InstructorClass::id).toList());
    Instant now = now();
    return scope.stream()
        .map(
            c ->
                new ClassRows(
                    c,
                    rows(
                        c.id(),
                        c.name(),
                        classes.listActiveRoster(c.id()),
                        windows.getOrDefault(c.id(), List.of()),
                        now)))
        .toList();
  }

  /** The figures of the given students of one class, in the order given. */
  List<ClassStudentRow> rows(
      UUID classId,
      String className,
      List<ClassRosterEntry> students,
      List<ClassProblemWindow> windows,
      Instant now) {
    if (students.isEmpty()) {
      return List.of();
    }
    List<StudentMetrics> answered;
    try {
      answered = metrics.read(classId, students, windows, ClassProgressRules.WEEKS);
    } catch (RuntimeException failure) {
      throw unavailable();
    }
    Map<UUID, StudentMetrics> byStudent = new HashMap<>();
    answered.forEach(m -> byStudent.put(m.studentId(), m));
    return students.stream()
        .map(
            s -> {
              StudentMetrics m = byStudent.get(s.studentId());
              if (m == null) {
                throw unavailable();
              }
              return ClassProgressRules.row(classId, className, s, m, now);
            })
        .toList();
  }
}
