package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.SampleRun.Reservation;
import java.util.UUID;

public interface SampleQuotaPort {
  Reservation reserve(UUID userId, UUID requestId);

  void refund(UUID userId, UUID requestId);
}
