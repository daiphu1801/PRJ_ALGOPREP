package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.ConfirmEmailChangeCommand;
import com.algoprep.identity.application.dto.PendingEmailChange;
import com.algoprep.identity.application.dto.RequestEmailChangeCommand;
import com.algoprep.identity.application.ports.in.ConfirmEmailChangeUseCase;
import com.algoprep.identity.application.ports.in.RequestEmailChangeUseCase;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated e-mail change #11 and #12. The caller comes from the token only. */
@RestController
@RequestMapping("/api/v1/identity/me/email-change")
class MyEmailController {
  private final RequestEmailChangeUseCase request;
  private final ConfirmEmailChangeUseCase confirm;

  MyEmailController(RequestEmailChangeUseCase request, ConfirmEmailChangeUseCase confirm) {
    this.request = request;
    this.confirm = confirm;
  }

  record RequestChangeBody(String newEmail) {}

  record ConfirmBody(String code) {}

  record EmailResponse(String email) {}

  @PostMapping("/request")
  PendingEmailChange request(
      @AuthenticationPrincipal Jwt jwt, @RequestBody RequestChangeBody body) {
    return request.request(
        new RequestEmailChangeCommand(UUID.fromString(jwt.getSubject()), body.newEmail()));
  }

  @PostMapping("/confirm")
  EmailResponse confirm(@AuthenticationPrincipal Jwt jwt, @RequestBody ConfirmBody body) {
    return new EmailResponse(
        confirm.confirm(
            new ConfirmEmailChangeCommand(UUID.fromString(jwt.getSubject()), body.code())));
  }
}
