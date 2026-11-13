package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.TokenUsageRecord;

/** Write side of the token ledger. */
public interface AiTokenUsageRepository {
  /** INSERT ... ON CONFLICT (call_id) DO NOTHING; true when a new row was written. */
  boolean insertIfAbsent(TokenUsageRecord usage);
}
