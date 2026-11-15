package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.CategoryCount;
import java.util.List;

/** Action counts per display group over the last seven days, all four groups (#65). */
public interface GetAuditCategoryBreakdownUseCase {
  List<CategoryCount> get();
}
