package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.application.ports.in.RecordAiTokenUsageUseCase;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Not transactional itself: every attempt opens its own transaction in {@link AiTokenLedgerWriter}
 * with the same {@code callId}, so a retry cannot double count (UNIQUE call_id, ON CONFLICT DO
 * NOTHING). After three failed attempts it logs ERROR with the call id and gives up; the caller
 * never sees a failure (DD F5 section 32.1).
 */
@Service
class RecordAiTokenUsageCommandHandler implements RecordAiTokenUsageUseCase {
  static final int MAX_ATTEMPTS = 3;
  private static final Logger LOG =
      Logger.getLogger(RecordAiTokenUsageCommandHandler.class.getName());
  private final AiTokenLedgerWriter writer;

  RecordAiTokenUsageCommandHandler(AiTokenLedgerWriter writer) {
    this.writer = writer;
  }

  @Override
  public void record(RecordAiTokenUsageCommand command) {
    if (!command.usageKnown()) {
      // Unknown usage writes no row and no zero; the provider may have billed us, which is a
      // published limit of the design (BR-AI-01).
      LOG.log(Level.FINE, "No usage reported for call {0}; nothing recorded", command.callId());
      return;
    }
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        writer.write(command);
        return;
      } catch (RuntimeException failure) {
        if (attempt == MAX_ATTEMPTS) {
          LOG.log(
              Level.SEVERE,
              "Token usage lost for call " + command.callId() + " after " + MAX_ATTEMPTS + " tries",
              failure);
        }
      }
    }
  }
}
