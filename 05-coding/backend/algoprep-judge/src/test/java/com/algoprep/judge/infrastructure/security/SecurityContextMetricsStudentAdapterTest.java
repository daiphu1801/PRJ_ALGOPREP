package com.algoprep.judge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextMetricsStudentAdapterTest {
  private final SecurityContextMetricsStudentAdapter adapter =
      new SecurityContextMetricsStudentAdapter();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private void jwt(String subject, String role) {
    var token =
        Jwt.withTokenValue("fixture")
            .header("alg", "RS256")
            .subject(subject)
            .claim("base_category", "ADMIN")
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @Test
  void currentAuthorityAndJwtSubjectDetermineTheStudentInsteadOfTheOldRoleClaim() {
    UUID student = UUID.randomUUID();
    jwt(student.toString(), "STUDENT");
    assertThat(adapter.requireStudent()).isEqualTo(student);
  }

  @Test
  void missingNonJwtAndNonStudentPrincipalsAreDenied() {
    assertThatThrownBy(adapter::requireStudent).isInstanceOf(AccessDeniedException.class);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "student", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    assertThatThrownBy(adapter::requireStudent).isInstanceOf(AccessDeniedException.class);
    jwt(UUID.randomUUID().toString(), "ADMIN");
    assertThatThrownBy(adapter::requireStudent).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void malformedSubjectsCannotEscapeTheControlledDenial() {
    jwt("private-invalid-subject", "STUDENT");
    assertThatThrownBy(adapter::requireStudent)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Student authentication required")
        .hasNoCause();
  }

  @Test
  void nullAuthorityNamesFailClosedWithoutANullPointerException() {
    var token =
        Jwt.withTokenValue("fixture")
            .header("alg", "RS256")
            .subject(UUID.randomUUID().toString())
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                token, List.<org.springframework.security.core.GrantedAuthority>of(() -> null)));
    assertThatThrownBy(adapter::requireStudent).isInstanceOf(AccessDeniedException.class);
  }
}
