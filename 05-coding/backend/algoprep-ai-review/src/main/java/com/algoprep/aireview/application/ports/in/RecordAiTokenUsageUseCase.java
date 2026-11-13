package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;

/** P2 (DD F5 section 32.1): writes one ledger row in its own transaction. Never throws. */
public interface RecordAiTokenUsageUseCase {
  void record(RecordAiTokenUsageCommand command);
}
