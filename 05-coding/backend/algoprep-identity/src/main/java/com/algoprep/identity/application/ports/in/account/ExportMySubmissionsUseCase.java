package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.SubmissionExport;
import java.util.UUID;

/** Exports the caller's own submission history, once per five minutes (#16, scope submissions). */
public interface ExportMySubmissionsUseCase {
  SubmissionExport export(UUID userId);
}
