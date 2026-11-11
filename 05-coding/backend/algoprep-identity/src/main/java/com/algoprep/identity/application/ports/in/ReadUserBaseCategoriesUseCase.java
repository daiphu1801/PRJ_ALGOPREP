package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.BaseCategory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Internal role lookup for neighbour modules. Unknown users are omitted from the answer. */
public interface ReadUserBaseCategoriesUseCase {
  Map<UUID, BaseCategory> getBaseCategories(Collection<UUID> userIds);
}
