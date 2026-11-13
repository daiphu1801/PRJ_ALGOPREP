package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListClassesSummaryUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassCard;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassesSummary;
import com.algoprep.identity.domain.model.ClassProgressRules;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.ClassStudentStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * #56. People are counted once across classes; a student is absent when absent in any class [SoT:
 * Suy luan, so that active plus absent equals the head count shown by INS0101].
 */
@Service
class ListClassesSummaryQueryHandler implements ListClassesSummaryUseCase {
  private final ClassMetricsAssembler assembler;

  ListClassesSummaryQueryHandler(ClassMetricsAssembler assembler) {
    this.assembler = assembler;
  }

  @Override
  public ClassesSummary summary(UUID instructorId) {
    var all = assembler.everyStudent(assembler.scope(instructorId, null));
    Set<UUID> people = new HashSet<>();
    Set<UUID> absent = new HashSet<>();
    List<ClassStudentRow> pairs = all.stream().flatMap(c -> c.rows().stream()).toList();
    for (ClassStudentRow r : pairs) {
      people.add(r.student().studentId());
      if (r.status() == ClassStudentStatus.ABSENT) {
        absent.add(r.student().studentId());
      }
    }
    List<ClassCard> cards =
        all.stream()
            .map(
                c ->
                    new ClassCard(
                        c.owned(),
                        ClassProgressRules.meanCompletionPercent(c.rows()),
                        ClassProgressRules.meanScore(c.rows()),
                        (int)
                            c.rows().stream()
                                .filter(r -> r.status() == ClassStudentStatus.ABSENT)
                                .count()))
            .toList();
    return new ClassesSummary(
        all.size(),
        people.size(),
        ClassProgressRules.meanCompletionPercent(pairs),
        absent.size(),
        cards);
  }
}
