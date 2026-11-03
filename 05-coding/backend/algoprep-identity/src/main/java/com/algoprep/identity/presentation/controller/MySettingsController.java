package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.UpdateMySettingsCommand;
import com.algoprep.identity.application.ports.in.GetMySettingsUseCase;
import com.algoprep.identity.application.ports.in.UpdateMySettingsUseCase;
import com.algoprep.identity.domain.model.MySettings;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated settings read #14 and edit #15. The caller comes from the token only. */
@RestController
@RequestMapping("/api/v1/identity/me/settings")
class MySettingsController {
  private final GetMySettingsUseCase get;
  private final UpdateMySettingsUseCase update;

  MySettingsController(GetMySettingsUseCase get, UpdateMySettingsUseCase update) {
    this.get = get;
    this.update = update;
  }

  /** A field left out (or null) keeps its stored value. */
  record UpdateSettingsRequest(
      Boolean streakReminderEnabled,
      Boolean weeklyReportEnabled,
      String preferredInterviewLevel,
      Integer preferredMaxTurns,
      Boolean preferredHintAllowed,
      String defaultLanguage) {}

  @GetMapping
  MySettings get(@AuthenticationPrincipal Jwt jwt) {
    return get.get(UUID.fromString(jwt.getSubject()));
  }

  @PatchMapping
  MySettings patch(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateSettingsRequest body) {
    return update.update(
        new UpdateMySettingsCommand(
            UUID.fromString(jwt.getSubject()),
            body.streakReminderEnabled(),
            body.weeklyReportEnabled(),
            body.preferredInterviewLevel(),
            body.preferredMaxTurns(),
            body.preferredHintAllowed(),
            body.defaultLanguage()));
  }
}
