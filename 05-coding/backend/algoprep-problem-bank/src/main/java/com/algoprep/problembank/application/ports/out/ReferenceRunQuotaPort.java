package com.algoprep.problembank.application.ports.out;

import java.util.UUID;

/**
 * The per-account allowance of the "run sample solution" button (BR-15): ten runs in a sliding 60
 * seconds. It never throws; if its store is down the run goes ahead.
 */
public interface ReferenceRunQuotaPort {
  /** 0 when the run is admitted and counted, otherwise the seconds until the next slot. */
  long reserve(UUID userId, UUID requestId);

  /** Gives the slot back after an infrastructure failure of the run itself. */
  void refund(UUID userId, UUID requestId);
}
