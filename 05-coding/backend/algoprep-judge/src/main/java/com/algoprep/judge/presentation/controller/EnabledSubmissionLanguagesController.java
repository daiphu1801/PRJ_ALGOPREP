package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.ports.in.GetEnabledJudgeLanguagesUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('STUDENT','INSTRUCTOR')")
class EnabledSubmissionLanguagesController {
  private final GetEnabledJudgeLanguagesUseCase languages;

  EnabledSubmissionLanguagesController(GetEnabledJudgeLanguagesUseCase languages) {
    this.languages = languages;
  }

  @GetMapping("/api/v1/judge/languages/enabled")
  Items enabled() {
    var enabled = languages.getEnabledLanguages();
    var result = List.of("PYTHON", "JAVA", "CPP").stream().filter(enabled::contains).toList();
    if (result.isEmpty()) {
      throw new JudgeReadUnavailableException();
    }
    return new Items(result);
  }

  record Items(List<String> items) {
    Items {
      items = List.copyOf(items);
    }
  }
}
