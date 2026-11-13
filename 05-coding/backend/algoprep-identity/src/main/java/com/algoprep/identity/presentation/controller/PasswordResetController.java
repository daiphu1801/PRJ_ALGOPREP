package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.ResetPasswordCommand;
import com.algoprep.identity.application.dto.VerifyPasswordResetOtpCommand;
import com.algoprep.identity.application.ports.in.ForgotPasswordUseCase;
import com.algoprep.identity.application.ports.in.ResetPasswordUseCase;
import com.algoprep.identity.application.ports.in.VerifyPasswordResetOtpUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public recovery endpoints #7/#60/#8. No session is issued by any of them. */
@RestController
@RequestMapping("/api/v1/identity/auth/password")
class PasswordResetController {
  private static final Logger LOG = LoggerFactory.getLogger(PasswordResetController.class);

  private final ForgotPasswordUseCase forgot;
  private final VerifyPasswordResetOtpUseCase verify;
  private final ResetPasswordUseCase reset;

  PasswordResetController(
      ForgotPasswordUseCase forgot,
      VerifyPasswordResetOtpUseCase verify,
      ResetPasswordUseCase reset) {
    this.forgot = forgot;
    this.verify = verify;
    this.reset = reset;
  }

  record ForgotRequest(String email) {}

  record MessageResponse(String message) {}

  record VerifiedResponse(boolean verified) {}

  @PostMapping("/forgot")
  MessageResponse forgot(@RequestBody ForgotRequest request) {
    try {
      forgot.forgot(request.email());
    } catch (MailException failure) {
      // The transaction has rolled back (no code, no audit row), so the person may ask again. The
      // answer must not differ from the one for an unknown address, or a broken mail server would
      // tell a caller which addresses have an account.
      LOG.warn("Password reset mail failed: {}", failure.getClass().getSimpleName());
    }
    return new MessageResponse("Nếu email tồn tại, mã xác nhận đã được gửi.");
  }

  @PostMapping("/verify-otp")
  VerifiedResponse verify(@RequestBody VerifyPasswordResetOtpCommand command) {
    verify.verify(command);
    return new VerifiedResponse(true);
  }

  @PostMapping("/reset")
  MessageResponse reset(@RequestBody ResetPasswordCommand command) {
    reset.reset(command);
    return new MessageResponse("Đặt lại mật khẩu thành công.");
  }
}
