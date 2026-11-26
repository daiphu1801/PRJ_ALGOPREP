package com.algoprep.problembank.presentation.controller;

import static com.algoprep.problembank.presentation.controller.ControllerSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * RBAC contract of every HTTP endpoint of the module: it carries a {@code @PreAuthorize}, an
 * authority-less caller is refused, and each authority the expression names is accepted on its own.
 * The expressions are evaluated by Spring Security itself, not parsed by the test.
 */
class EndpointGuardTest {
  private static final Pattern AUTHORITY = Pattern.compile("'([A-Z_]+:[A-Z_]+)'");
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final List<Class<?>> CONTROLLERS =
      List.of(
          AiGenerationSettingsController.class,
          AuthoringDefaultsController.class,
          ClassAssignmentController.class,
          MyClassAssignmentController.class,
          ProblemAuthoringBulkController.class,
          ProblemAuthoringController.class,
          ProblemAuthoringHelpersController.class,
          ProblemAuthoringListController.class,
          ProblemLevelsController.class,
          ProblemSpecController.class,
          ProblemTestcaseBulkController.class,
          ProblemTestcaseController.class,
          ProblemTestcaseGenerationController.class,
          ProblemTopicsController.class,
          PublishedProblemController.class,
          StudentCatalogController.class,
          SuggestedProblemController.class);

  /**
   * The only endpoints a guest may call (DD F2 section 0A, table 0A.1). Each says so with an
   * explicit {@code permitAll()}; a new open endpoint has to be added here on purpose.
   */
  private static final Set<String> GUEST_OPEN =
      Set.of(
          "PublishedProblemController#list",
          "PublishedProblemController#random",
          "PublishedProblemController#overview",
          "PublishedProblemController#samples",
          "ProblemLevelsController#list");

  private final PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager();

  private static boolean isEndpoint(Method m) {
    return m.isAnnotationPresent(GetMapping.class)
        || m.isAnnotationPresent(PostMapping.class)
        || m.isAnnotationPresent(PutMapping.class)
        || m.isAnnotationPresent(PatchMapping.class)
        || m.isAnnotationPresent(DeleteMapping.class);
  }

  private static String expression(Class<?> type, Method m) {
    PreAuthorize own = m.getAnnotation(PreAuthorize.class);
    PreAuthorize shared = AnnotatedElementUtils.findMergedAnnotation(type, PreAuthorize.class);
    return own != null ? own.value() : shared == null ? null : shared.value();
  }

  static Stream<Arguments> endpoints() {
    var all = new ArrayList<Arguments>();
    for (Class<?> type : CONTROLLERS) {
      for (Method m : type.getDeclaredMethods()) {
        if (isEndpoint(m)) {
          all.add(Arguments.of(type.getSimpleName() + "#" + m.getName(), type, m));
        }
      }
    }
    return all.stream();
  }

  @Test
  void theControllerListCoversEveryRestControllerOfTheModule() {
    assertThat(endpoints().count()).isGreaterThan(55);
  }

  private boolean allows(
      Class<?> type, Method m, org.springframework.security.core.Authentication a) {
    MethodInvocation invocation = new SimpleMethodInvocation(mock(type), m);
    return manager.authorize(() -> a, invocation).isGranted();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("endpoints")
  void everyEndpointIsGuardedAndRefusesACallerWithoutTheRequiredAuthority(
      String name, Class<?> type, Method method) {
    String expression = expression(type, method);

    assertThat(expression).as(name).isNotNull();
    if (GUEST_OPEN.contains(name)) {
      assertThat(expression).as(name).isEqualTo("permitAll()");
      assertThat(allows(type, method, anonymous())).as(name + " anonymous").isTrue();
      return;
    }
    assertThat(GUEST_OPEN).as(name).doesNotContain(name);
    assertThat(expression).as(name + " is not left open").doesNotContain("permitAll");
    boolean anyAuthenticated = expression.contains("isAuthenticated()");
    assertThat(allows(type, method, jwt(USER, "UNRELATED:READ")))
        .as(name)
        .isEqualTo(anyAuthenticated);
    assertThat(allows(type, method, anonymous())).as(name + " anonymous").isFalse();
  }

  private static org.springframework.security.core.Authentication anonymous() {
    return new AnonymousAuthenticationToken(
        "key", "anon", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
  }

  @Test
  void theGuestOpenSetNamesExistingEndpointsOnly() {
    var names = new java.util.HashSet<String>();
    endpoints().forEach(a -> names.add((String) a.get()[0]));
    assertThat(names).containsAll(GUEST_OPEN);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("endpoints")
  void everyAuthorityNamedByTheExpressionIsAcceptedOnItsOwn(
      String name, Class<?> type, Method method) {
    String expression = expression(type, method);
    if (GUEST_OPEN.contains(name)) {
      return;
    }
    Matcher matcher = AUTHORITY.matcher(expression);
    var named = new ArrayList<String>();
    while (matcher.find()) {
      named.add(matcher.group(1));
    }
    if (expression.contains("hasRole('STUDENT')")) {
      assertThat(allows(type, method, jwt(USER, "ROLE_STUDENT"))).as(name).isTrue();
      assertThat(allows(type, method, jwt(USER, "ROLE_INSTRUCTOR"))).as(name).isFalse();
    }
    if (expression.contains("isAuthenticated()")) {
      assertThat(allows(type, method, jwt(USER))).as(name).isTrue();
    }
    for (String authority : named) {
      assertThat(allows(type, method, jwt(USER, authority))).as(name + " " + authority).isTrue();
    }
    assertThat(named.isEmpty() && !expression.contains("hasRole") && !expression.contains("isAuth"))
        .as(name + " has a recognisable guard")
        .isFalse();
  }

  // ---- error advices ----

  @Test
  void authoringAdviceMapsDeniedAndMalformedInputToStableProblemDetails() {
    var advice = new ProblemAuthoringErrorAdvice();

    assertProblem(advice.denied(), HttpStatus.FORBIDDEN, "PRB-002");
    assertProblem(advice.invalidParameter(), HttpStatus.BAD_REQUEST, "PRB-001");
    assertProblem(advice.unbindable(), HttpStatus.BAD_REQUEST, "PRB-001");
    assertProblem(advice.invalidBody(), HttpStatus.BAD_REQUEST, "PRB-001");
  }

  @Test
  void catalogAndSpecAdvicesMapDeniedAndUnreadableBodies() {
    var catalog = new CatalogErrorAdvice();
    var spec = new ProblemSpecErrorAdvice();

    assertProblem(catalog.denied(), HttpStatus.FORBIDDEN, "PRB-002");
    assertProblem(catalog.invalidBody(), HttpStatus.BAD_REQUEST, "PRB-001");
    assertProblem(spec.denied(), HttpStatus.FORBIDDEN, "PRB-002");
    assertProblem(spec.invalidBody(), HttpStatus.BAD_REQUEST, "PRB-001");
    assertThat(spec.invalidBody().getDetail()).contains("specification");
    assertThat(catalog.invalidBody().getDetail()).contains("catalogue");
  }

  @Test
  void theAdviceHandlersAreBoundToTheExceptionTypesTheyName() throws Exception {
    assertThat(
            ProblemAuthoringErrorAdvice.class
                .getDeclaredMethod("denied")
                .getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class)
                .value())
        .containsExactly(AccessDeniedException.class);
    assertThat(
            ProblemAuthoringErrorAdvice.class
                .getDeclaredMethod("invalidParameter")
                .getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class)
                .value())
        .containsExactly(MethodArgumentTypeMismatchException.class);
    assertThat(
            CatalogErrorAdvice.class
                .getDeclaredMethod("invalidBody")
                .getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class)
                .value())
        .containsExactly(HttpMessageNotReadableException.class);
  }

  private static void assertProblem(ProblemDetail detail, HttpStatus status, String code) {
    assertThat(detail.getStatus()).isEqualTo(status.value());
    assertThat(detail.getProperties()).containsEntry("errorCode", code);
  }
}
