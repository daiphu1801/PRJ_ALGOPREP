package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetClassScoreTrendUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.ScoreTrend;
import com.algoprep.identity.domain.model.ClassMetricViews.TrendPoint;
import com.algoprep.identity.domain.model.ClassProgressRules;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.StudentMetrics;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * #57. ponytail: no F1 cache, the judge already caches its figures for 15 minutes; add one in F1 if
 * the dashboard proves slow.
 */
@Service
class GetClassScoreTrendQueryHandler implements GetClassScoreTrendUseCase {
  private final ClassMetricsAssembler assembler;

  GetClassScoreTrendQueryHandler(ClassMetricsAssembler assembler) {
    this.assembler = assembler;
  }

  @Override
  public ScoreTrend trend(UUID instructorId, UUID classId) {
    var scope = assembler.scope(instructorId, classId);
    List<ClassStudentRow> rows =
        assembler.everyStudent(scope).stream().flatMap(c -> c.rows().stream()).toList();
    List<Double> scores = ClassProgressRules.weeklyScores(rows);
    List<LocalDate> starts =
        rows.stream()
            .filter(r -> r.metrics().weeks().size() == scores.size())
            .findFirst()
            .map(
                r -> r.metrics().weeks().stream().map(StudentMetrics.WeekPoint::weekStart).toList())
            .orElseGet(() -> ClassProgressRules.weekStarts(assembler.now()));
    List<TrendPoint> points = new ArrayList<>(scores.size());
    for (int i = 0; i < scores.size(); i++) {
      points.add(new TrendPoint(starts.get(i), scores.get(i)));
    }
    return new ScoreTrend(classId, classId == null ? null : scope.get(0).name(), points);
  }
}
