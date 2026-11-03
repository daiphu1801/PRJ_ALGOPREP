package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.ports.in.GetAdminQueueSettingsUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueueAutoscaleUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueuePausedUseCase;
import com.algoprep.judge.application.ports.in.UpdateAdminQueueSettingsUseCase;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge/admin/queue")
class AdminQueueControlController {
  private final GetAdminQueueSettingsUseCase reader;
  private final SetAdminQueuePausedUseCase pause;
  private final SetAdminQueueAutoscaleUseCase autoscale;
  private final UpdateAdminQueueSettingsUseCase settings;

  AdminQueueControlController(
      GetAdminQueueSettingsUseCase reader,
      SetAdminQueuePausedUseCase pause,
      SetAdminQueueAutoscaleUseCase autoscale,
      UpdateAdminQueueSettingsUseCase settings) {
    this.reader = reader;
    this.pause = pause;
    this.autoscale = autoscale;
    this.settings = settings;
  }

  @GetMapping("/settings")
  @PreAuthorize("hasAuthority('JUDGE_QUEUE_MONITOR:READ')")
  AdminQueueSettings read() {
    return reader.read();
  }

  @PutMapping("/settings")
  @PreAuthorize("hasRole('ADMIN')")
  AdminQueueSettings update(
      @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) String request) {
    return settings.update(UUID.fromString(jwt.getSubject()), AdminQueueRequest.settings(request));
  }

  @PutMapping("/consumption")
  @PreAuthorize("hasRole('ADMIN')")
  PausedResponse paused(
      @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) String request) {
    var body = AdminQueueRequest.body(request, "JDG-V16");
    var result =
        pause.setPaused(
            UUID.fromString(jwt.getSubject()),
            AdminQueueRequest.bool(body, "expectedPaused"),
            AdminQueueRequest.bool(body, "paused"));
    return new PausedResponse(result.value(), result.updatedAt());
  }

  @PutMapping("/autoscale")
  @PreAuthorize("hasRole('ADMIN')")
  EnabledResponse autoscale(
      @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) String request) {
    var body = AdminQueueRequest.body(request, "JDG-V16");
    var result =
        autoscale.setAutoscale(
            UUID.fromString(jwt.getSubject()),
            AdminQueueRequest.bool(body, "expectedEnabled"),
            AdminQueueRequest.bool(body, "enabled"));
    return new EnabledResponse(result.value(), result.updatedAt());
  }

  record PausedResponse(boolean paused, Instant updatedAt) {}

  record EnabledResponse(boolean enabled, Instant updatedAt) {}
}
