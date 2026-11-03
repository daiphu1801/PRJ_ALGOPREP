package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;
import com.algoprep.identity.application.ports.in.ChangeMyPasswordUseCase;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated password change #13 (BR-06). Needs a bearer token; see SecurityConfig. */
@RestController
@RequestMapping("/api/v1/identity/me")
class MyPasswordController {
  private final ChangeMyPasswordUseCase change;

  MyPasswordController(ChangeMyPasswordUseCase change) {
    this.change = change;
  }

  record ChangePasswordRequest(String currentPassword, String newPassword) {}

  record MessageResponse(String message) {}

  @PostMapping("/password")
  MessageResponse change(
      @AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest body) {
    // A token issued before "sid" existed has no session: 401 makes the client refresh and retry.
    String sid = jwt.getClaimAsString("sid");
    if (sid == null) {
      throw new InvalidBearerTokenException("Token has no session id");
    }
    change.change(
        new ChangeMyPasswordCommand(
            UUID.fromString(jwt.getSubject()),
            UUID.fromString(sid),
            body.currentPassword(),
            body.newPassword()));
    return new MessageResponse("Đổi mật khẩu thành công.");
  }
}
