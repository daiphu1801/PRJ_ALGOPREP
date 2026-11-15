package com.algoprep.judge.application.ports.in;

import java.util.List;

public interface GetEnabledJudgeLanguagesUseCase {
  List<String> getEnabledLanguages();
}
