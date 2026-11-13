package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.AuthoringDefaults;
import com.algoprep.problembank.application.ports.in.ReadAuthoringDefaultsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problem-bank/authoring/defaults")
class AuthoringDefaultsController {
  private final ReadAuthoringDefaultsUseCase defaults;

  AuthoringDefaultsController(ReadAuthoringDefaultsUseCase defaults) {
    this.defaults = defaults;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('PROBLEM_AUTHORING:CREATE', 'PROBLEM_AUTHORING:READ')")
  AuthoringDefaults get() {
    return defaults.get();
  }
}
