package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.ListInstructorRecentActivityUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Class activity feed #45; a bad {@code classId} is 404 IDT-414, never a different error. */
@RestController
@RequestMapping("/api/v1/identity/instructor/dashboard")
class InstructorActivityController {
  private final ListInstructorRecentActivityUseCase activity;

  InstructorActivityController(ListInstructorRecentActivityUseCase activity) {
    this.activity = activity;
  }

  record Item(
      UUID id,
      String activityType,
      UUID actorUserId,
      String actorDisplayName,
      UUID scopeClassId,
      String refType,
      UUID refId,
      String occurredAt) {}

  record Response(List<Item> items) {}

  @GetMapping("/recent-activity")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  Response recentActivity(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String classId) {
    UUID scope = classId == null ? null : ClassPath.id(classId);
    return new Response(
        activity.list(UUID.fromString(jwt.getSubject()), scope).stream()
            .map(
                e ->
                    new Item(
                        e.id(),
                        e.activityType(),
                        e.actorUserId(),
                        e.actorDisplayName(),
                        e.scopeClassId(),
                        e.refType(),
                        e.refId(),
                        e.occurredAt().toString()))
            .toList());
  }
}
