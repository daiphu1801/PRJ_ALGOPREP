package com.algoprep.identity.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.InstructorStudentsQuery;
import com.algoprep.identity.application.ports.in.ListInstructorStudentsUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ClassMetricViews.StudentPage;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * #59. The page is cut from the rosters first; the judge is asked only for the students of that
 * page (DD api F1 section 0.8.3). A bad {@code q} or bad paging is 400 IDT-V17.
 */
@Service
class ListInstructorStudentsQueryHandler implements ListInstructorStudentsUseCase {
  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;
  static final int KEYWORD_MAX = 100;

  private record Pair(InstructorClass owned, ClassRosterEntry student) {}

  private record Key(UUID classId, UUID studentId) {}

  private final InstructorClassRepository classes;
  private final ClassMetricsAssembler assembler;

  ListInstructorStudentsQueryHandler(
      InstructorClassRepository classes, ClassMetricsAssembler assembler) {
    this.classes = classes;
    this.assembler = assembler;
  }

  @Override
  public StudentPage list(InstructorStudentsQuery query) {
    String keyword = query.keyword() == null ? "" : query.keyword().trim();
    int page = query.page() == null ? 1 : query.page();
    int size = query.pageSize() == null ? DEFAULT_PAGE_SIZE : query.pageSize();
    if (keyword.length() > KEYWORD_MAX || page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new InvalidRequestException(
          IdentityErrorCodes.LIST_PARAMETER_INVALID, "A filter or paging value is not valid");
    }
    String needle = keyword.toLowerCase(Locale.ROOT);
    List<Pair> pairs = new ArrayList<>();
    for (InstructorClass c : assembler.scope(query.instructorId(), query.classId())) {
      for (ClassRosterEntry s : classes.listActiveRoster(c.id())) {
        if (needle.isEmpty() || s.displayName().toLowerCase(Locale.ROOT).contains(needle)) {
          pairs.add(new Pair(c, s));
        }
      }
    }
    pairs.sort(
        Comparator.comparing((Pair p) -> p.student().displayName().toLowerCase(Locale.ROOT))
            .thenComparing(p -> p.owned().id())
            .thenComparing(p -> p.student().studentId()));
    long total = pairs.size();
    int from = (int) Math.min((long) (page - 1) * size, total);
    List<Pair> slice = pairs.subList(from, (int) Math.min(from + (long) size, total));
    return new StudentPage(rowsOf(slice), page, size, total, (int) ((total + size - 1) / size));
  }

  private List<ClassStudentRow> rowsOf(List<Pair> slice) {
    if (slice.isEmpty()) {
      return List.of();
    }
    List<InstructorClass> pageClasses = slice.stream().map(Pair::owned).distinct().toList();
    Map<UUID, List<ClassProblemWindow>> windows =
        assembler.windows(pageClasses.stream().map(InstructorClass::id).toList());
    Instant now = assembler.now();
    Map<Key, ClassStudentRow> byKey = new HashMap<>();
    for (InstructorClass c : pageClasses) {
      List<ClassRosterEntry> students =
          slice.stream().filter(p -> p.owned().id().equals(c.id())).map(Pair::student).toList();
      assembler
          .rows(c.id(), c.name(), students, windows.getOrDefault(c.id(), List.of()), now)
          .forEach(r -> byKey.put(new Key(r.classId(), r.student().studentId()), r));
    }
    return slice.stream()
        .map(p -> byKey.get(new Key(p.owned().id(), p.student().studentId())))
        .toList();
  }
}
