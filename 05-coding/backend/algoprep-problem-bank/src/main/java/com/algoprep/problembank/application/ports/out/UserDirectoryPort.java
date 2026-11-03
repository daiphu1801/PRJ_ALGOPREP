package com.algoprep.problembank.application.ports.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Internal attribution lookup; absent entries map to null display names in authoring DTOs. */
public interface UserDirectoryPort {
  Map<UUID, String> getDisplayNames(Collection<UUID> userIds);
}
