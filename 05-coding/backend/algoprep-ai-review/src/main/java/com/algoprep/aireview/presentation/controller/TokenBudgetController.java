package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.ports.in.GetAiLimitsConfigUseCase;
import com.algoprep.aireview.application.ports.in.GetAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.in.SetAiTokenLimitUseCase;
import com.algoprep.aireview.application.ports.in.UnlockAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.in.UpdateAiLimitsConfigUseCase;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * DD api F5 sections 18 to 22 (ADM0302, ADM0303). Budget limit and unlock need {@code
 * AI_TOKEN_BUDGET:UPDATE}; {@code AI_CONFIG:UPDATE} is not enough (SEC5:68-71). Rate limits need
 * {@code AI_CONFIG:UPDATE}. The two reads accept either READ cell. All need the ADMIN category.
 */
@RestController
@RequestMapping("/api/v1/ai/admin")
class TokenBudgetController {
  private static final String ADMIN = "hasAuthority('ROLE_ADMIN')";
  private static final String READ_EITHER =
      ADMIN + " and (hasAuthority('AI_TOKEN_BUDGET:READ') or hasAuthority('AI_CONFIG:READ'))";
  private static final String BUDGET_UPDATE = ADMIN + " and hasAuthority('AI_TOKEN_BUDGET:UPDATE')";
  private static final String CONFIG_UPDATE = ADMIN + " and hasAuthority('AI_CONFIG:UPDATE')";

  private final GetAiTokenBudgetUseCase status;
  private final SetAiTokenLimitUseCase setLimit;
  private final UnlockAiTokenBudgetUseCase unlock;
  private final GetAiLimitsConfigUseCase readLimits;
  private final UpdateAiLimitsConfigUseCase updateLimits;

  TokenBudgetController(
      GetAiTokenBudgetUseCase status,
      SetAiTokenLimitUseCase setLimit,
      UnlockAiTokenBudgetUseCase unlock,
      GetAiLimitsConfigUseCase readLimits,
      UpdateAiLimitsConfigUseCase updateLimits) {
    this.status = status;
    this.setLimit = setLimit;
    this.unlock = unlock;
    this.readLimits = readLimits;
    this.updateLimits = updateLimits;
  }

  record FeatureTokens(FeatureCode featureCode, long tokens) {}

  record Budget(
      long limitTokens,
      long usedTokens,
      LocalDate periodStart,
      Instant lockedAt,
      boolean isLocked,
      LocalDate projectedRunOutDate,
      List<FeatureTokens> featureBreakdown,
      int warnPercent) {}

  record Limits(int reviewPerHour, int interviewPerHour, int cooldownSeconds, int warnPercent) {}

  private static Budget budget(BudgetStatus s) {
    return new Budget(
        s.limitTokens(),
        s.usedTokens(),
        s.periodStart(),
        s.lockedAt(),
        s.locked(),
        s.projectedRunOutDate(),
        s.featureBreakdown().entrySet().stream()
            .map(e -> new FeatureTokens(e.getKey(), e.getValue()))
            .toList(),
        s.warnPercent());
  }

  private static Limits limits(LimitsConfig c) {
    return new Limits(
        c.reviewPerHour(), c.interviewPerHour(), c.cooldownSeconds(), c.warnPercent());
  }

  private static UUID actor(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  @GetMapping("/token-budget")
  @PreAuthorize(READ_EITHER)
  Budget budget() {
    return budget(status.status());
  }

  @PutMapping("/token-budget/limit")
  @PreAuthorize(BUDGET_UPDATE)
  Budget setLimit(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return budget(setLimit.setLimit(actor(caller), BudgetRequests.limitTokens(body)));
  }

  @PostMapping("/token-budget/unlock")
  @PreAuthorize(BUDGET_UPDATE)
  Budget unlock(JwtAuthenticationToken caller) {
    return budget(unlock.unlock(actor(caller)));
  }

  @GetMapping("/limits-config")
  @PreAuthorize(READ_EITHER)
  Limits limits() {
    return limits(readLimits.read());
  }

  @PatchMapping("/limits-config")
  @PreAuthorize(CONFIG_UPDATE)
  Limits updateLimits(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    return limits(updateLimits.update(actor(caller), BudgetRequests.limits(body)));
  }
}
