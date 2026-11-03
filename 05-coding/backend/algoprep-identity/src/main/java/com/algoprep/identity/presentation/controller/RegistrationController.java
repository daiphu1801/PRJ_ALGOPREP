package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.application.dto.RegistrationResponse;
import com.algoprep.identity.application.dto.VerifyEmailCommand;
import com.algoprep.identity.application.ports.in.RegisterUseCase;
import com.algoprep.identity.application.ports.in.ResendEmailVerificationUseCase;
import com.algoprep.identity.application.ports.in.VerifyEmailUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints #1, #53 and #54; never issue a session. */
@RestController
@RequestMapping("/api/v1/identity/auth")
class RegistrationController {
  private final RegisterUseCase registration;
  private final VerifyEmailUseCase verification;
  private final ResendEmailVerificationUseCase resend;

  RegistrationController(
      RegisterUseCase registration,
      VerifyEmailUseCase verification,
      ResendEmailVerificationUseCase resend) {
    this.registration = registration;
    this.verification = verification;
    this.resend = resend;
  }

  record MessageResponse(String message) {}

  record ResendRequest(String email) {}

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  RegistrationResponse register(@RequestBody RegisterCommand command) {
    return registration.register(command);
  }

  @PostMapping("/email/verify")
  MessageResponse verify(@RequestBody VerifyEmailCommand command) {
    verification.verify(command);
    return new MessageResponse("Xác thực email thành công.");
  }

  @PostMapping("/email/verify/resend")
  MessageResponse resend(@RequestBody ResendRequest request) {
    resend.resend(request.email());
    return new MessageResponse("Nếu email cần xác thực, mã đã được gửi");
  }
}
