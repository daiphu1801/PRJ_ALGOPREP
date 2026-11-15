package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.AdminLanguageConfig;

/** Reads the complete admin language configuration, API #21. */
public interface GetAdminLanguageConfigUseCase {
  AdminLanguageConfig read();
}
