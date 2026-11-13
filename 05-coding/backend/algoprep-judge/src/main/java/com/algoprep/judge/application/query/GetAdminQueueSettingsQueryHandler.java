package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetAdminQueueSettingsUseCase;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetAdminQueueSettingsQueryHandler implements GetAdminQueueSettingsUseCase {
  private final AdminQueueRepository repository;

  GetAdminQueueSettingsQueryHandler(AdminQueueRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminQueueSettings read() {
    return repository.read().settings();
  }
}
