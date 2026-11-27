package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.application.dto.audit.CategoryCount;
import java.util.List;

/** Action counts per display group over the last seven days, all four groups (#65). */
public interface GetAuditCategoryBreakdownUseCase {
  List<CategoryCount> get();
}
