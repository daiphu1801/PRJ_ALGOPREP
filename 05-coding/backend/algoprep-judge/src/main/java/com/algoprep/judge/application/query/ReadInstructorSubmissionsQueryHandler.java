package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.SubmissionReads.ClassHistory;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.InstructorDetail;
import com.algoprep.judge.application.dto.SubmissionReads.InstructorItem;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import com.algoprep.judge.application.ports.in.GetInstructorSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.ListInstructorSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListStudentClassSubmissionsUseCase;
import com.algoprep.judge.domain.ports.out.InstructorSubmissionRepository;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class ReadInstructorSubmissionsQueryHandler
    implements ListInstructorSubmissionsUseCase,
        GetInstructorSubmissionDetailUseCase,
        ListStudentClassSubmissionsUseCase {
  private final InstructorSubmissionScope scope;
  private final InstructorSubmissionRepository repository;
  private final SubmissionReadRepository submissions;
  private final SubmissionReadProjection projection;

  ReadInstructorSubmissionsQueryHandler(
      InstructorSubmissionScope scope,
      InstructorSubmissionRepository repository,
      SubmissionReadRepository submissions,
      SubmissionReadProjection projection) {
    this.scope = scope;
    this.repository = repository;
    this.submissions = submissions;
    this.projection = projection;
  }

  @Override
  public Page<InstructorItem> list(UUID instructor, Filters f) {
    var context = scope.load(instructor);
    if (f.classId() != null) {
      context.requireClass(f.classId());
    }
    var filter = context.filter(f.classId(), null, f.verdict(), f.q());
    long count = repository.count(filter);
    int page = SubmissionReadQueries.page(f.page(), count);
    var rows =
        repository.list(filter, (page - 1) * 20, 20).stream()
            .map(
                s -> {
                  var m = context.member(s);
                  return new InstructorItem(
                      s.id(),
                      s.studentId(),
                      m.studentName(),
                      s.classId(),
                      m.className(),
                      s.problemId(),
                      context.title(s),
                      s.language(),
                      s.status(),
                      s.score() == null ? null : s.passed(),
                      s.score() == null ? null : s.total(),
                      s.submittedAt(),
                      m.leftAt() != null);
                })
            .toList();
    return new Page<>(rows, page, 20, count);
  }

  // Calls F2 over a REQUIRES_NEW read; it must not run inside a transaction that holds a
  // connection,
  // otherwise every request pins one connection while waiting for a second and the pool deadlocks.
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public InstructorDetail detail(UUID instructor, UUID id) {
    var context = scope.load(instructor);
    var rows = repository.list(context.filter(null, id, null, null), 0, 1);
    if (rows.isEmpty()) {
      throw SubmissionReadQueries.missing("JDG-110");
    }
    var state = submissions.find(id).orElseThrow(() -> SubmissionReadQueries.missing("JDG-110"));
    return new InstructorDetail(projection.detail(state, false), context.member(rows.getFirst()));
  }

  @Override
  public Page<ClassHistory> classHistory(
      UUID instructor, UUID classId, UUID studentId, String requestedPage) {
    var context = scope.load(instructor).student(classId, studentId);
    var filter = context.filter(classId, null, null, null);
    long count = repository.count(filter);
    int page = SubmissionReadQueries.page(requestedPage, count);
    var rows =
        repository.list(filter, (page - 1) * 20, 20).stream()
            .map(
                s ->
                    new ClassHistory(
                        s.id(),
                        context.title(s),
                        s.language(),
                        s.status(),
                        s.score(),
                        s.submittedAt()))
            .toList();
    return new Page<>(rows, page, 20, count);
  }
}
