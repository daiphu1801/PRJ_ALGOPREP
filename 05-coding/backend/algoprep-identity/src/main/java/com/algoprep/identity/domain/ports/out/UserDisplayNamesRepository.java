package com.algoprep.identity.domain.ports.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads only identifiers and display names; never restores anonymized personal data. */
public interface UserDisplayNamesRepository {
  Map<UUID, String> findDisplayNames(Set<UUID> userIds);
}
