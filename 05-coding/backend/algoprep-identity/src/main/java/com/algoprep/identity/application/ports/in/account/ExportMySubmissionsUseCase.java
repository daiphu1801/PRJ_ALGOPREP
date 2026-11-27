package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.SubmissionExport;
import java.util.UUID;

/** Exports the caller's own submission history, once per five minutes (#16, scope submissions). */
public interface ExportMySubmissionsUseCase {
  SubmissionExport export(UUID userId);
}
