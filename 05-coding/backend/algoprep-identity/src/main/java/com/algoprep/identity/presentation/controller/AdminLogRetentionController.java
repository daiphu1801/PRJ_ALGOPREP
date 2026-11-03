package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.UpdateLogRetentionCommand;
import com.algoprep.identity.application.ports.in.GetLogRetentionSettingsUseCase;
import com.algoprep.identity.application.ports.in.UpdateLogRetentionSettingsUseCase;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import java.math.BigInteger;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit log retention read #51 (any ADMIN) and edit #52 (ADMIN with the {@code SYSTEM_AUDIT_LOG}
 * cell, DD api F1 section 0.7.1).
 */
@RestController
@RequestMapping("/api/v1/identity/admin/settings/log-retention")
class AdminLogRetentionController {
  private final GetLogRetentionSettingsUseCase get;
  private final UpdateLogRetentionSettingsUseCase update;

  AdminLogRetentionController(
      GetLogRetentionSettingsUseCase get, UpdateLogRetentionSettingsUseCase update) {
    this.get = get;
    this.update = update;
  }

  /**
   * {@code retentionDays} is read as a plain value so a string or a fraction is a 422 {@code
   * IDT-442} from the handler instead of an unreadable-body 400.
   */
  record UpdateRequest(Object retentionDays, String expiredPolicy) {}

  record RetentionResponse(int retentionDays, String expiredPolicy) {
    static RetentionResponse of(LogRetentionSettings s) {
      return new RetentionResponse(s.retentionDays(), s.expiredPolicy().name());
    }
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  RetentionResponse read() {
    return RetentionResponse.of(get.get());
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SYSTEM_AUDIT_LOG:UPDATE')")
  RetentionResponse edit(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateRequest body) {
    return RetentionResponse.of(
        update.update(
            new UpdateLogRetentionCommand(
                UUID.fromString(jwt.getSubject()),
                wholeNumber(body.retentionDays()),
                body.expiredPolicy())));
  }

  private static Long wholeNumber(Object sent) {
    if (sent instanceof Integer || sent instanceof Long) {
      return ((Number) sent).longValue();
    }
    return sent instanceof BigInteger ? Long.MAX_VALUE : null;
  }
}
