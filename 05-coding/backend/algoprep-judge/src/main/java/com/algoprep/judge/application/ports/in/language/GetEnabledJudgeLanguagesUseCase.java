package com.algoprep.judge.application.ports.in.language;

import java.util.List;

public interface GetEnabledJudgeLanguagesUseCase {
  List<String> getEnabledLanguages();
}
