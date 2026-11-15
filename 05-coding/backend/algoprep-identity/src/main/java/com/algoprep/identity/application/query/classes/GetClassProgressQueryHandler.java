package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetClassProgressUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassProgress;
import com.algoprep.identity.domain.model.ClassProgressRules;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.ClassStudentStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** #29: mean completion (BR-12) and the head count of each label (BR-10). */
@Service
class GetClassProgressQueryHandler implements GetClassProgressUseCase {
  private final ClassMetricsAssembler assembler;

  GetClassProgressQueryHandler(ClassMetricsAssembler assembler) {
    this.assembler = assembler;
  }

  @Override
  public ClassProgress get(UUID instructorId, UUID classId) {
    var one = assembler.everyStudent(assembler.scope(instructorId, classId)).get(0);
    List<ClassStudentRow> rows = one.rows();
    Map<ClassStudentStatus, Integer> counts = new EnumMap<>(ClassStudentStatus.class);
    for (ClassStudentStatus s : ClassStudentStatus.values()) {
      counts.put(s, (int) rows.stream().filter(r -> r.status() == s).count());
    }
    return new ClassProgress(
        classId,
        one.owned().name(),
        rows.size(),
        ClassProgressRules.meanCompletionPercent(rows),
        ClassProgressRules.meanScore(rows),
        counts);
  }
}
