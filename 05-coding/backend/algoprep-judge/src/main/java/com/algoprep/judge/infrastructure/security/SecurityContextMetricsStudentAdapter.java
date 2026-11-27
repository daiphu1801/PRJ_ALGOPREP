package com.algoprep.judge.infrastructure.security;

import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class SecurityContextMetricsStudentAdapter implements CurrentMetricsStudentPort {
  @Override
  public UUID requireStudent() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwt)
        || !jwt.isAuthenticated()
        || jwt.getAuthorities().stream().noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))) {
      throw denied();
    }
    try {
      return UUID.fromString(jwt.getToken().getSubject());
    } catch (RuntimeException invalid) {
      throw denied();
    }
  }

  private static AccessDeniedException denied() {
    return new AccessDeniedException("Student authentication required");
  }
}
