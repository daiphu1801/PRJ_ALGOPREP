package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.InfraEvents.Filter;
import com.algoprep.judge.domain.model.InfraEvents.Row;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import java.util.List;
import java.util.Map;

/** SELECT-only, one shared UNION source for the summary and event list. */
public interface InfraEventRepository {
  Summary summary();

  List<Row> list(Filter filter, Map<String, String> templates);
}
