package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignableItem;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignableList;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignedClass;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignedItem;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignmentList;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.Summary;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.TopItem;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.TopList;
import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.application.ports.in.ReadClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.out.ClassProblemMetricsPort;
import com.algoprep.problembank.application.ports.out.InstructorClassPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ActiveRow;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.Metric;
import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentRepository;
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
 * DD F2 sections 50, 51, 52 and 55. Ownership comes from identity; the numbers come from the judge
 * module and are null (or an empty block) when it cannot answer, never a made-up zero.
 */
@Service
class ClassAssignmentQueryHandler implements ReadClassAssignmentsUseCase {
  static final int PAGE_SIZE = 20;
  static final int TOP_SIZE = 5;

  private final ClassAssignmentRepository assignments;
  private final ClassAssignmentReadRepository history;
  private final InstructorClassPort classes;
  private final ClassProblemMetricsPort metrics;

  ClassAssignmentQueryHandler(
      ClassAssignmentRepository assignments,
      ClassAssignmentReadRepository history,
      InstructorClassPort classes,
      ClassProblemMetricsPort metrics) {
    this.assignments = assignments;
    this.history = history;
    this.classes = classes;
    this.metrics = metrics;
  }

  @Override
  @Transactional(readOnly = true)
  public AssignmentList list(UUID instructorId, Map<String, String> params) {
    var owned = classes.classesOf(instructorId);
    var names = names(owned);
    UUID tab = classId(params.get("classId"));
    if (tab != null && !names.containsKey(tab)) {
      throw notYours();
    }
    Set<UUID> scope = tab == null ? names.keySet() : Set.of(tab);
    var rows = assignments.active(scope, text(params.get("q")));
    var grouped = new LinkedHashMap<UUID, List<ActiveRow>>();
    rows.stream()
        .sorted(Comparator.comparing(ActiveRow::assignedAt).reversed())
        .forEach(r -> grouped.computeIfAbsent(r.problemId(), k -> new ArrayList<>()).add(r));
    var ordered =
        grouped.values().stream()
            .sorted(
                Comparator.comparing(
                        (List<ActiveRow> g) ->
                            g.stream().map(ActiveRow::assignedAt).max(Instant::compareTo).get())
                    .reversed())
            .toList();
    int totalPages = Math.max(1, (int) Math.ceil(ordered.size() / (double) PAGE_SIZE));
    int page = Math.min(Math.max(integer(params.get("page")), 1), totalPages);
    var pageGroups = ordered.stream().skip((long) (page - 1) * PAGE_SIZE).limit(PAGE_SIZE).toList();
    var pageIds = pageGroups.stream().map(g -> g.get(0).problemId()).collect(Collectors.toSet());
    var numbers = metrics.read(instructorId, scope, pageIds, historyOf(names.keySet(), pageIds));
    var items = new ArrayList<AssignedItem>();
    for (var group : pageGroups) {
      var first = group.get(0);
      var metric = numbers.map(m -> m.get(first.problemId())).orElse(null);
      items.add(
          new AssignedItem(
              first.problemId(),
              first.title(),
              first.topicNames(),
              first.levelCode(),
              first.levelDisplayName(),
              group.stream()
                  .map(r -> new AssignedClass(r.classId(), names.get(r.classId())))
                  .toList(),
              numbers.isPresent() ? count(metric) : null,
              metric == null ? null : metric.acRatePercent(),
              first.withdrawn()));
    }
    return new AssignmentList(items, new PageMeta(page, PAGE_SIZE, ordered.size(), totalPages));
  }

  @Override
  @Transactional(readOnly = true)
  public Summary summary(UUID instructorId) {
    var names = names(classes.classesOf(instructorId));
    var problemIds =
        assignments.active(names.keySet(), null).stream()
            .map(ActiveRow::problemId)
            .collect(Collectors.toSet());
    Integer average = null;
    if (!problemIds.isEmpty()) {
      var numbers =
          metrics.read(
              instructorId, names.keySet(), problemIds, historyOf(names.keySet(), problemIds));
      var rates =
          numbers.map(
              m -> m.values().stream().map(Metric::acRatePercent).filter(r -> r != null).toList());
      if (rates.isPresent() && !rates.get().isEmpty()) {
        average =
            (int)
                Math.round(
                    rates.get().stream().mapToDouble(Double::doubleValue).average().orElse(0));
      }
    }
    return new Summary(problemIds.size(), names.size(), average);
  }

  @Override
  @Transactional(readOnly = true)
  public AssignableList assignable(UUID instructorId, UUID classId, Map<String, String> params) {
    if (!names(classes.classesOf(instructorId)).containsKey(classId)) {
      throw notYours();
    }
    int requested = Math.max(integer(params.get("page")), 1);
    var found = assignments.assignable(classId, text(params.get("q")), requested, PAGE_SIZE);
    int totalPages = Math.max(1, (int) Math.ceil(found.totalItems() / (double) PAGE_SIZE));
    var items =
        found.items().stream()
            .map(
                a ->
                    new AssignableItem(
                        a.problemId(),
                        a.code(),
                        a.title(),
                        a.levelCode(),
                        a.levelDisplayName(),
                        a.topicNames()))
            .toList();
    return new AssignableList(
        items, new PageMeta(found.page(), PAGE_SIZE, found.totalItems(), totalPages));
  }

  @Override
  @Transactional(readOnly = true)
  public TopList top(UUID instructorId) {
    var names = names(classes.classesOf(instructorId));
    var rows = assignments.active(names.keySet(), null);
    var titles = new LinkedHashMap<UUID, String>();
    rows.forEach(r -> titles.putIfAbsent(r.problemId(), r.title()));
    if (titles.isEmpty()) {
      return new TopList(List.of());
    }
    var numbers =
        metrics.read(
            instructorId,
            names.keySet(),
            titles.keySet(),
            historyOf(names.keySet(), titles.keySet()));
    if (numbers.isEmpty()) {
      return new TopList(List.of());
    }
    var items =
        titles.entrySet().stream()
            .map(e -> new TopItem(e.getKey(), e.getValue(), count(numbers.get().get(e.getKey()))))
            .sorted(
                Comparator.comparingLong(TopItem::submissionCount)
                    .reversed()
                    .thenComparing(TopItem::title))
            .limit(TOP_SIZE)
            .toList();
    return new TopList(items);
  }

  private List<ClassAssignmentSnapshot> historyOf(Set<UUID> classIds, Set<UUID> problemIds) {
    return history.findAssignments(classIds).stream()
        .filter(h -> problemIds.contains(h.problemId()))
        .toList();
  }

  private static Map<UUID, String> names(List<ClassRef> owned) {
    var names = new LinkedHashMap<UUID, String>();
    owned.forEach(c -> names.put(c.id(), c.name()));
    return names;
  }

  private static long count(Metric metric) {
    return metric == null ? 0 : metric.submissionCount();
  }

  private static PermissionDeniedException notYours() {
    return new PermissionDeniedException("PRB-005", "You are not responsible for this class");
  }

  private static UUID classId(String raw) {
    try {
      return raw == null || raw.isBlank() ? null : UUID.fromString(raw.trim());
    } catch (IllegalArgumentException e) {
      // A class id that is not a UUID cannot be one of the caller's classes.
      return new UUID(0, 0);
    }
  }

  private static String text(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
  }

  private static int integer(String raw) {
    try {
      return raw == null ? 1 : Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return 1;
    }
  }
}
