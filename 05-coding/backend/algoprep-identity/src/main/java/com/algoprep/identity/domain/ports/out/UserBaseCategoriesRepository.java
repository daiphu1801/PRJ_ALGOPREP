package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.BaseCategory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads only identifiers and the base category of the role each user holds. */
public interface UserBaseCategoriesRepository {
  Map<UUID, BaseCategory> findBaseCategories(Set<UUID> userIds);
}
