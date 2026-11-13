package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetLogRetentionSettingsUseCase;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Any ADMIN may read; the gate is on the controller. */
@Service
class GetLogRetentionSettingsQueryHandler implements GetLogRetentionSettingsUseCase {
  private final LogRetentionRepository retention;

  GetLogRetentionSettingsQueryHandler(LogRetentionRepository retention) {
    this.retention = retention;
  }

  @Override
  @Transactional(readOnly = true)
  public LogRetentionSettings get() {
    return retention.read();
  }
}
