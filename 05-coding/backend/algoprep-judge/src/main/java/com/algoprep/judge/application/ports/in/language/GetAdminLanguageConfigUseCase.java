package com.algoprep.judge.application.ports.in.language;

import com.algoprep.judge.domain.model.language.AdminLanguageConfig;

/** Reads the complete admin language configuration, API #21. */
public interface GetAdminLanguageConfigUseCase {
  AdminLanguageConfig read();
}
