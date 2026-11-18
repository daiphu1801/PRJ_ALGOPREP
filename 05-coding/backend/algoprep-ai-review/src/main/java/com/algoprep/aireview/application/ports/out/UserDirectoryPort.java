package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Outbound port served by identity (DD F5 section 1.3, 2 second deadline in the bridge). */
public interface UserDirectoryPort {
  /** Unknown ids are absent. Any failure is a RuntimeException (callers degrade to null names). */
  Map<UUID, String> getDisplayNames(Collection<UUID> userIds);

  /** Unknown ids are absent. Any failure is a RuntimeException (the anomaly run is skipped). */
  Map<UUID, CallerCategory> getBaseCategories(Collection<UUID> userIds);
}
