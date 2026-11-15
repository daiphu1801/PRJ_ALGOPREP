package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.PersonalDataExport;
import java.util.UUID;

/** Exports the caller's own profile data, once per five minutes (#16, scope profile). */
public interface ExportMyProfileDataUseCase {
  PersonalDataExport export(UUID userId);
}
