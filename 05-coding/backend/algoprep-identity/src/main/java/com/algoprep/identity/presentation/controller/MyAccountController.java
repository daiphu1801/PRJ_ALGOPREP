package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.DeleteMyAccountUseCase;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated self-deletion #18. The caller comes from the token only. */
@RestController
@RequestMapping("/api/v1/identity/me/account")
class MyAccountController {
  private final DeleteMyAccountUseCase delete;

  MyAccountController(DeleteMyAccountUseCase delete) {
    this.delete = delete;
  }

  record DeleteRequest(String emailConfirmation) {}

  record MessageResponse(String message) {}

  @PostMapping("/delete")
  MessageResponse delete(@AuthenticationPrincipal Jwt jwt, @RequestBody DeleteRequest body) {
    delete.delete(UUID.fromString(jwt.getSubject()), body.emailConfirmation());
    return new MessageResponse("Tài khoản đã được đặt vào thời gian chờ xoá 30 ngày.");
  }
}
