package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.LoginCommand;
import com.algoprep.identity.application.dto.LoginResult;
import com.algoprep.identity.application.dto.RefreshResult;
import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.ports.in.LoginUseCase;
import com.algoprep.identity.application.ports.in.LogoutUseCase;
import com.algoprep.identity.application.ports.in.RefreshSessionUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session endpoints #2, #49, #3 and #4 (DD api F1 sections 0.1 and 1).
 *
 * <p>The refresh token lives only in an HttpOnly cookie scoped to this path. Because the browser
 * attaches that cookie by itself, {@code /refresh} also demands a custom header, which a cross-site
 * form post cannot add (BD security section 1).
 */
@RestController
@RequestMapping(AuthController.BASE_PATH)
class AuthController {

  static final String BASE_PATH = "/api/v1/identity/auth";
  static final String REFRESH_COOKIE = "refresh_token";

  private final LoginUseCase login;
  private final RefreshSessionUseCase refresh;
  private final LogoutUseCase logout;
  private final boolean secureCookie;
  private final Duration cookieMaxAge;

  AuthController(
      LoginUseCase login,
      RefreshSessionUseCase refresh,
      LogoutUseCase logout,
      @Value("${algoprep.security.refresh-cookie.secure:true}") boolean secureCookie,
      @Value("${algoprep.security.refresh-ttl-days:30}") long refreshTtlDays) {
    this.login = login;
    this.refresh = refresh;
    this.logout = logout;
    this.secureCookie = secureCookie;
    this.cookieMaxAge = Duration.ofDays(refreshTtlDays);
  }

  /** Request body of both login endpoints. */
  record LoginRequest(String email, String password) {}

  /** Response body of both login endpoints. */
  record LoginResponse(String accessToken, long expiresIn, UserSummary user) {}

  /** Response body of the refresh endpoint. */
  record RefreshResponse(String accessToken, long expiresIn) {}

  @PostMapping("/login")
  ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
    return loginAs(request, false, http.getRemoteAddr());
  }

  @PostMapping("/instructor/login")
  ResponseEntity<LoginResponse> instructorLogin(
      @RequestBody LoginRequest request, HttpServletRequest http) {
    return loginAs(request, true, http.getRemoteAddr());
  }

  @PostMapping("/refresh")
  ResponseEntity<RefreshResponse> refresh(
      @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
    if (requestedWith == null || requestedWith.isBlank()) {
      throw new InvalidRequestException(
          IdentityErrorCodes.MISSING_REQUESTED_WITH, "X-Requested-With header is required");
    }
    RefreshResult result = refresh.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(result.rawRefreshToken(), cookieMaxAge).toString())
        .body(new RefreshResponse(result.accessToken(), result.expiresInSeconds()));
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
    logout.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
        .build();
  }

  private ResponseEntity<LoginResponse> loginAs(
      LoginRequest request, boolean instructorPortal, String sourceIp) {
    LoginResult result =
        login.login(
            new LoginCommand(request.email(), request.password(), instructorPortal, sourceIp));
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(result.rawRefreshToken(), cookieMaxAge).toString())
        .body(new LoginResponse(result.accessToken(), result.expiresInSeconds(), result.user()));
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(REFRESH_COOKIE, value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Strict")
        .path(BASE_PATH)
        .maxAge(maxAge)
        .build();
  }
}
