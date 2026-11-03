package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetAdminLanguageConfigUseCase;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetAdminLanguageConfigQueryHandler implements GetAdminLanguageConfigUseCase {
  private final AdminLanguageConfigRepository repository;

  GetAdminLanguageConfigQueryHandler(AdminLanguageConfigRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AdminLanguageConfig read() {
    return repository.read();
  }
}
