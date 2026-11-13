package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListClassAttentionStudentsUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.AttentionStudent;
import com.algoprep.identity.domain.model.ClassProgressRules;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** #58: the ordering lives in {@link ClassProgressRules#attention}. */
@Service
class ListClassAttentionStudentsQueryHandler implements ListClassAttentionStudentsUseCase {
  private final ClassMetricsAssembler assembler;

  ListClassAttentionStudentsQueryHandler(ClassMetricsAssembler assembler) {
    this.assembler = assembler;
  }

  @Override
  public List<AttentionStudent> list(UUID instructorId, UUID classId) {
    var all = assembler.everyStudent(assembler.scope(instructorId, classId));
    return ClassProgressRules.attention(
        all.stream().flatMap(c -> c.rows().stream()).toList(), assembler.now());
  }
}
