package com.algoprep.identity.application.ports.in;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Internal attribution lookup. Unknown users are omitted; anonymized users have a safe label. */
public interface ReadUserDisplayNamesUseCase {
  Map<UUID, String> getDisplayNames(Collection<UUID> userIds);
}
