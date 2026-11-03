package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.ports.in.RestoreDeactivatedAccountUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public ticket redemption endpoint; it never creates a login session. */
@RestController
@RequestMapping("/api/v1/identity/auth/deactivation")
class AccountRestorationController {
  private final RestoreDeactivatedAccountUseCase restoration;

  AccountRestorationController(RestoreDeactivatedAccountUseCase restoration) {
    this.restoration = restoration;
  }

  record RestoredResponse(boolean restored) {}

  @PostMapping("/restore")
  RestoredResponse restore(@RequestBody RestoreAccountCommand command) {
    restoration.restore(command);
    return new RestoredResponse(true);
  }
}
