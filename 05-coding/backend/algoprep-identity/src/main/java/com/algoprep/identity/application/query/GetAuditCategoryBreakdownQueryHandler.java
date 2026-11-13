package com.algoprep.identity.application.query;

import com.algoprep.identity.application.dto.CategoryCount;
import com.algoprep.identity.application.ports.in.GetAuditCategoryBreakdownUseCase;
import com.algoprep.identity.domain.model.AuditActionCatalog;
import com.algoprep.identity.domain.model.AuditCategory;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #65: counts per display group over seven days. The database groups by raw action type (a small
 * set); the catalog folds those into the four groups here, so the grouping rule lives in one place
 * and all four groups are always present, with 0 when empty.
 */
@Service
class GetAuditCategoryBreakdownQueryHandler implements GetAuditCategoryBreakdownUseCase {
  private final AuditLogReadRepository logs;

  GetAuditCategoryBreakdownQueryHandler(AuditLogReadRepository logs) {
    this.logs = logs;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryCount> get() {
    Map<AuditCategory, Long> totals = new EnumMap<>(AuditCategory.class);
    for (AuditCategory category : AuditCategory.values()) {
      totals.put(category, 0L);
    }
    logs.actionCountsLastSevenDays()
        .forEach(
            (action, count) ->
                totals.merge(AuditActionCatalog.resolve(action).category(), count, Long::sum));
    return totals.entrySet().stream()
        .map(e -> new CategoryCount(e.getKey().name(), e.getValue()))
        .toList();
  }
}
