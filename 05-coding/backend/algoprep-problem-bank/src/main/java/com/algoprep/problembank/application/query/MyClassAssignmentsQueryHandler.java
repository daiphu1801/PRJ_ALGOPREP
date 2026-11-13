package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.StudentClassViews.ClassProblemsMeta;
import com.algoprep.problembank.application.dto.StudentClassViews.MyAssignmentGroup;
import com.algoprep.problembank.application.dto.StudentClassViews.MyAssignmentGroups;
import com.algoprep.problembank.application.dto.StudentClassViews.MyAssignmentItem;
import com.algoprep.problembank.application.dto.StudentClassViews.MyClassProblem;
import com.algoprep.problembank.application.dto.StudentClassViews.MyClassProblems;
import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.application.ports.in.MyClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.out.MyClassAssignmentStatePort;
import com.algoprep.problembank.application.ports.out.StudentEnrollmentPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import com.algoprep.problembank.domain.model.StudentClassModels.MyAssignment;
import com.algoprep.problembank.domain.ports.out.StudentClassAssignmentReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 sections 5 and 5A. Both start from the classes the caller is enrolled in now; a state read
 * that fails leaves {@code solveState} null in section 5 and is a 503 in section 5A, never a
 * guessed "unsolved".
 */
@Service
class MyClassAssignmentsQueryHandler implements MyClassAssignmentsUseCase {
  static final int MAX_GROUPS = 3;
  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 50;

  private final StudentClassAssignmentReadRepository repository;
  private final StudentEnrollmentPort enrollment;
  private final MyClassAssignmentStatePort states;

  MyClassAssignmentsQueryHandler(
      StudentClassAssignmentReadRepository repository,
      StudentEnrollmentPort enrollment,
      MyClassAssignmentStatePort states) {
    this.repository = repository;
    this.enrollment = enrollment;
    this.states = states;
  }

  @Override
  @Transactional(readOnly = true)
  public MyAssignmentGroups groups(UUID userId) {
    var classes = enrollment.activeClassesOf(userId).orElse(List.of());
    if (classes.isEmpty()) {
      return new MyAssignmentGroups(List.of());
    }
    var names = names(classes);
    var byClass = new LinkedHashMap<UUID, List<MyAssignment>>();
    repository.active(names.keySet(), true).stream()
        .sorted(newestFirst())
        .forEach(a -> byClass.computeIfAbsent(a.classId(), k -> new ArrayList<>()).add(a));
    var kept =
        byClass.entrySet().stream()
            .sorted(
                Comparator.comparing(
                        (Map.Entry<UUID, List<MyAssignment>> e) -> e.getValue().get(0).assignedAt(),
                        Comparator.<Instant>reverseOrder())
                    .thenComparing(e -> e.getKey().toString()))
            .limit(MAX_GROUPS)
            .toList();
    var all = kept.stream().flatMap(e -> e.getValue().stream()).toList();
    var found = states.read(keys(all)).orElse(null);
    return new MyAssignmentGroups(
        kept.stream()
            .map(e -> new MyAssignmentGroup(e.getKey(), names.get(e.getKey()), items(e, found)))
            .toList());
  }

  @Override
  @Transactional(readOnly = true)
  public MyClassProblems problems(UUID userId, String classId, String page, String pageSize) {
    UUID filter = classId == null ? null : classId(classId);
    var classes =
        enrollment
            .activeClassesOf(userId)
            .orElseThrow(
                () ->
                    new ServiceUnavailableException(
                        "PRB-058", "Class information is unavailable", null));
    Set<UUID> enrolled = names(classes).keySet();
    if (filter != null && !enrolled.contains(filter)) {
      throw notFound();
    }
    Set<UUID> scope = filter == null ? enrolled : Set.of(filter);
    var rows = scope.isEmpty() ? List.<MyAssignment>of() : repository.active(scope, false);
    int size = pageSizeOf(pageSize);
    if (rows.isEmpty()) {
      return new MyClassProblems(
          List.of(), new PageMeta(1, size, 0, 1), new ClassProblemsMeta(0, 0));
    }
    var order =
        filter == null
            ? Comparator.comparing((MyAssignment a) -> a.classId().toString())
                .thenComparing(newestFirst())
            : newestFirst();
    // ponytail: pages in memory, bounded by one learner's class assignments; SQL paging if that
    // grows
    var sorted = rows.stream().sorted(order).toList();
    int totalPages = (sorted.size() + size - 1) / size;
    int number = Math.min(Math.max(positive(page, 1), 1), totalPages);
    var ordered = sorted.stream().skip((long) (number - 1) * size).limit(size).toList();
    Map<AssignmentKey, ClassSolveState> found =
        states
            .read(keys(ordered))
            .orElseThrow(
                () ->
                    new ServiceUnavailableException("PRB-057", "Solve state is unavailable", null));
    return new MyClassProblems(
        pageOf(ordered, found),
        new PageMeta(number, size, sorted.size(), totalPages),
        new ClassProblemsMeta(sorted.size(), solvedCount(sorted, ordered, found)));
  }

  /**
   * Solved rows over the whole list. The page read is reused when it already covers every row;
   * otherwise one more P7 call covers all rows, and its failure only nulls the count.
   */
  private Integer solvedCount(
      List<MyAssignment> all,
      List<MyAssignment> page,
      Map<AssignmentKey, ClassSolveState> pageStates) {
    // ponytail: one P7 call with every row of the learner's classes (P7 has no documented batch
    // cap); chunk the keys if a cap is ever set
    var known = all.size() == page.size() ? pageStates : states.read(keys(all)).orElse(null);
    if (known == null) {
      return null;
    }
    var solved = 0;
    for (var a : all) {
      var s = state(known, a);
      if (s == null) {
        return null;
      }
      if (s == ClassSolveState.SOLVED) {
        solved++;
      }
    }
    return solved;
  }

  private static List<MyClassProblem> pageOf(
      List<MyAssignment> ordered, Map<AssignmentKey, ClassSolveState> found) {
    return ordered.stream()
        .map(
            a ->
                new MyClassProblem(
                    a.classId(),
                    a.problemId(),
                    a.code(),
                    a.title(),
                    a.levelDisplayName(),
                    state(found, a),
                    a.assignedAt(),
                    a.withdrawn()))
        .toList();
  }

  /** Student reads are lenient: a missing, malformed or out-of-range size means the default. */
  private static int pageSizeOf(String raw) {
    int size = positive(raw, DEFAULT_PAGE_SIZE);
    return size > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : size;
  }

  private static int positive(String raw, int fallback) {
    try {
      int value = raw == null ? fallback : Integer.parseInt(raw.trim());
      return value < 1 ? fallback : value;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static List<MyAssignmentItem> items(
      Map.Entry<UUID, List<MyAssignment>> group, Map<AssignmentKey, ClassSolveState> found) {
    return group.getValue().stream()
        .map(a -> new MyAssignmentItem(a.problemId(), a.title(), state(found, a), a.assignedAt()))
        .toList();
  }

  /** Newest assignment first; the id string orders like the database does, bytes unsigned. */
  private static Comparator<MyAssignment> newestFirst() {
    return Comparator.comparing(MyAssignment::assignedAt, Comparator.<Instant>reverseOrder())
        .thenComparing(a -> a.problemId().toString());
  }

  private static Map<UUID, String> names(List<ClassRef> classes) {
    return classes.stream()
        .collect(Collectors.toMap(ClassRef::id, ClassRef::name, (a, b) -> a, LinkedHashMap::new));
  }

  private static List<AssignmentKey> keys(List<MyAssignment> rows) {
    return rows.stream()
        .map(a -> new AssignmentKey(a.classId(), a.problemId(), a.assignedAt()))
        .toList();
  }

  private static ClassSolveState state(Map<AssignmentKey, ClassSolveState> found, MyAssignment a) {
    return found == null
        ? null
        : found.get(new AssignmentKey(a.classId(), a.problemId(), a.assignedAt()));
  }

  private static UUID classId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw notFound();
    }
  }

  private static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("PRB-015", "Class not found");
  }
}
