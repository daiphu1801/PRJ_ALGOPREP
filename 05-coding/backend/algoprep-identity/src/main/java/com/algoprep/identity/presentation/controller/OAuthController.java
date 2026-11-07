package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.LoginResult;
import com.algoprep.identity.application.dto.OAuthStart;
import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.ports.in.CompleteOAuthLoginUseCase;
import com.algoprep.identity.application.ports.in.StartOAuthLoginUseCase;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth #5 and #6 (DD F1 section 0.1, BR-03). Public routes: the callback answers exactly like a
 * password login, with the refresh token only in the HttpOnly cookie.
 */
@RestController
@RequestMapping("/api/v1/identity/auth/oauth")
class OAuthController {
  private final StartOAuthLoginUseCase start;
  private final CompleteOAuthLoginUseCase complete;
  private final boolean secureCookie;
  private final Duration cookieMaxAge;

  OAuthController(
      StartOAuthLoginUseCase start,
      CompleteOAuthLoginUseCase complete,
      @Value("${algoprep.security.refresh-cookie.secure:true}") boolean secureCookie,
      @Value("${algoprep.security.refresh-ttl-days:30}") long ttlDays) {
    this.start = start;
    this.complete = complete;
    this.secureCookie = secureCookie;
    this.cookieMaxAge = Duration.ofDays(ttlDays);
  }

  record LoginResponse(String accessToken, long expiresIn, UserSummary user) {}

  @GetMapping("/{provider}/redirect")
  OAuthStart redirect(@PathVariable String provider) {
    return start.start(provider);
  }

  @GetMapping("/{provider}/callback")
  ResponseEntity<LoginResponse> callback(
      @PathVariable String provider,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state) {
    LoginResult result = complete.complete(provider, code, state);
    // Same cookie as the password login (AuthController): HttpOnly, Strict, scoped to the auth
    // path.
    ResponseCookie cookie =
        ResponseCookie.from(AuthController.REFRESH_COOKIE, result.rawRefreshToken())
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Strict")
            .path(AuthController.BASE_PATH)
            .maxAge(cookieMaxAge)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new LoginResponse(result.accessToken(), result.expiresInSeconds(), result.user()));
  }
}
