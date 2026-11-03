package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.UpdateMyProfileCommand;
import com.algoprep.identity.application.ports.in.GetMyProfileUseCase;
import com.algoprep.identity.application.ports.in.UpdateMyProfileUseCase;
import com.algoprep.identity.domain.model.MyProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated profile read #9 and edit #10. The caller comes from the token; no id is accepted,
 * and {@code email}, role and status in a PATCH body are ignored.
 */
@RestController
@RequestMapping("/api/v1/identity/me")
class MyProfileController {
  private final GetMyProfileUseCase profile;
  private final UpdateMyProfileUseCase update;

  MyProfileController(GetMyProfileUseCase profile, UpdateMyProfileUseCase update) {
    this.profile = profile;
    this.update = update;
  }

  /** A field left out (or null) keeps its value; an empty string clears an optional one. */
  record UpdateProfileRequest(
      String displayName,
      String schoolOrCompany,
      String currentPosition,
      String targetPosition,
      String phone) {}

  /** Timestamps are ISO-8601 UTC strings (DD api F1 section 0.2.1). */
  record ProfileResponse(
      UUID userId,
      String displayName,
      String email,
      String schoolOrCompany,
      String currentPosition,
      String targetPosition,
      String defaultLanguage,
      String phone,
      boolean hasPassword,
      String passwordChangedAt,
      List<String> linkedProviders,
      String joinedAt,
      String roleName,
      boolean mustChangePassword) {

    static ProfileResponse of(MyProfile p) {
      return new ProfileResponse(
          p.userId(),
          p.displayName(),
          p.email(),
          p.schoolOrCompany(),
          p.currentPosition(),
          p.targetPosition(),
          p.defaultLanguage(),
          p.phone(),
          p.hasPassword(),
          p.passwordChangedAt() == null ? null : p.passwordChangedAt().toString(),
          p.linkedProviders(),
          p.joinedAt().toString(),
          p.roleName(),
          p.mustChangePassword());
    }
  }

  @GetMapping("/profile")
  ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
    return ProfileResponse.of(profile.get(UUID.fromString(jwt.getSubject())));
  }

  @PatchMapping("/profile")
  ProfileResponse patch(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest body) {
    return ProfileResponse.of(
        update.update(
            new UpdateMyProfileCommand(
                UUID.fromString(jwt.getSubject()),
                body.displayName(),
                body.schoolOrCompany(),
                body.currentPosition(),
                body.targetPosition(),
                body.phone())));
  }
}
