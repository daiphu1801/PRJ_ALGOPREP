package com.algoprep.harness.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.StarterCodeResult;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class StarterCodeControllerTest {
  private final GetStarterCodeUseCase port = mock(GetStarterCodeUseCase.class);
  private final StarterCodeController controller = new StarterCodeController(port);
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();

  private JwtAuthenticationToken caller(String liveRole) {
    var jwt =
        Jwt.withTokenValue("fixture")
            .header("alg", "RS256")
            .subject(user.toString())
            .claim("base_category", "ADMIN")
            .build();
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(liveRole)));
  }

  @Test
  void translatesOnlyPublicFieldsAndUsesLiveAuthoritiesInsteadOfStaleAdminClaim() {
    when(port.getStarterCode(any()))
        .thenReturn(
            new StarterCodeResult(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, "starter", 42));
    var result =
        controller.get(
            caller("ROLE_STUDENT"), problem.toString(), "JAVA", SubmissionModel.FUNCTION_WRAPPER);
    assertThat(result.sourceCode()).isEqualTo("starter");
    assertThat(result.language()).isEqualTo(Language.JAVA);
    assertThat(result.submissionModel()).isEqualTo(SubmissionModel.FUNCTION_WRAPPER);
    var query = ArgumentCaptor.forClass(GetStarterCodeQuery.class);
    verify(port).getStarterCode(query.capture());
    assertThat(query.getValue().callerUserId()).isEqualTo(user);
    assertThat(query.getValue().baseCategory()).isEqualTo("STUDENT");
    assertThat(query.getValue().problemId()).isEqualTo(problem);
  }

  @Test
  void malformedIdAndMissingOrUnsupportedLanguageNeverCallThePort() {
    assertThatThrownBy(
            () ->
                controller.get(caller("ROLE_STUDENT"), "bad", "JAVA", SubmissionModel.STANDARD_IO))
        .satisfies(e -> code(e, "HRN-001"));
    for (String language : new String[] {null, "", "GO", "java"}) {
      assertThatThrownBy(
              () ->
                  controller.get(
                      caller("ROLE_STUDENT"),
                      problem.toString(),
                      language,
                      SubmissionModel.STANDARD_IO))
          .satisfies(e -> code(e, "HRN-010"));
    }
    verifyNoInteractions(port);
  }

  @Test
  void noLiveBaseRoleCannotReuseTheTokenClaim() {
    assertThatThrownBy(
            () ->
                controller.get(
                    caller("SOME_FUNCTION:READ"),
                    problem.toString(),
                    "JAVA",
                    SubmissionModel.STANDARD_IO))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(port);
  }

  private static void code(Throwable error, String expected) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
  }
}
