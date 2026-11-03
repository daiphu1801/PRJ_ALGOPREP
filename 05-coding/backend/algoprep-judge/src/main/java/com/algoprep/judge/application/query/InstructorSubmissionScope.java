package com.algoprep.judge.application.query;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.judge.application.dto.ClassAssignment;
import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import com.algoprep.judge.application.ports.out.ClassAssignmentReadPort;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.domain.model.InstructorReadFilter;
import com.algoprep.judge.domain.model.InstructorReadWindow;
import com.algoprep.judge.domain.model.ScopedSubmission;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class InstructorSubmissionScope {
  private final ClassScopeReadPort scope;
  private final ClassAssignmentReadPort assignments;

  InstructorSubmissionScope(ClassScopeReadPort scope, ClassAssignmentReadPort assignments) {
    this.scope = scope;
    this.assignments = assignments;
  }

  Context load(UUID instructor) {
    try {
      var members = scope.read(instructor);
      var assigned =
          assignments.listAssignments(
              members.stream().map(Membership::classId).collect(Collectors.toSet()));
      return new Context(members, assigned);
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          com.algoprep.judge.domain.exception.SubmissionReadErrorCodes.UNAVAILABLE,
          "Submission scope unavailable",
          null);
    }
  }

  record Context(List<Membership> members, List<ClassAssignment> assignments) {
    void requireClass(UUID classId) {
      if (members.stream().noneMatch(m -> m.classId().equals(classId))) {
        throw SubmissionReadQueries.missing("JDG-111");
      }
    }

    Context student(UUID classId, UUID studentId) {
      requireClass(classId);
      var selected =
          members.stream()
              .filter(m -> m.classId().equals(classId) && studentId.equals(m.studentId()))
              .toList();
      if (selected.isEmpty()) {
        throw SubmissionReadQueries.missing("JDG-112");
      }
      return new Context(
          selected, assignments.stream().filter(a -> a.classId().equals(classId)).toList());
    }

    InstructorReadFilter filter(UUID classId, UUID id, String result, String keyword) {
      var windows =
          members.stream()
              .filter(m -> m.studentId() != null)
              .flatMap(
                  m ->
                      assignments.stream()
                          .filter(a -> a.classId().equals(m.classId()))
                          .map(
                              a ->
                                  new InstructorReadWindow(
                                      m.classId(),
                                      m.studentId(),
                                      a.problemId(),
                                      m.joinedAt(),
                                      m.leftAt(),
                                      a.assignedAt(),
                                      a.removedAt())))
              .toList();
      String q = SubmissionReadQueries.keyword(keyword).toLowerCase(Locale.ROOT);
      var students =
          q.isEmpty()
              ? null
              : members.stream()
                  .filter(
                      m ->
                          m.studentId() != null
                              && m.studentName().toLowerCase(Locale.ROOT).contains(q))
                  .map(Membership::studentId)
                  .distinct()
                  .toList();
      var problems =
          q.isEmpty()
              ? null
              : assignments.stream()
                  .filter(a -> a.problemTitle().toLowerCase(Locale.ROOT).contains(q))
                  .map(ClassAssignment::problemId)
                  .distinct()
                  .toList();
      return new InstructorReadFilter(windows, classId, id, result, students, problems);
    }

    Membership member(ScopedSubmission row) {
      return members.stream()
          .filter(m -> m.classId().equals(row.classId()) && row.studentId().equals(m.studentId()))
          .findFirst()
          .orElseThrow();
    }

    String title(ScopedSubmission row) {
      return assignments.stream()
          .filter(
              a ->
                  a.classId().equals(row.classId())
                      && a.problemId().equals(row.problemId())
                      && !a.assignedAt().isAfter(row.submittedAt())
                      && (a.removedAt() == null || row.submittedAt().isBefore(a.removedAt())))
          .max(Comparator.comparing(ClassAssignment::assignedAt))
          .orElseThrow()
          .problemTitle();
    }
  }
}
