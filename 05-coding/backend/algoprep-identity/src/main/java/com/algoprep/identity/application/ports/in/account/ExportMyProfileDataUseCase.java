package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.PersonalDataExport;
import java.util.UUID;

/** Exports the caller's own profile data, once per five minutes (#16, scope profile). */
public interface ExportMyProfileDataUseCase {
  PersonalDataExport export(UUID userId);
}
