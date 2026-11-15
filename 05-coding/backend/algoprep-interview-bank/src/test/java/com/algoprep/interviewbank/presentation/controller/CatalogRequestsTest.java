package com.algoprep.interviewbank.presentation.controller;

import static com.algoprep.interviewbank.presentation.controller.ControllerFixtures.caller;
import static com.algoprep.interviewbank.presentation.controller.ControllerFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Catalogue write shapes: the role is checked first (INB-302), then the body (INB-V03, V13, V14).
 */
class CatalogRequestsTest {
  private final UUID actorId = UUID.randomUUID();
  private final JwtAuthenticationToken admin = caller(actorId, "ROLE_ADMIN");
  private final JwtAuthenticationToken instructor = caller(actorId, "ROLE_INSTRUCTOR");
  private final UUID id = UUID.randomUUID();

  private static void rejects(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void adminFlagAndActorComeFromTheToken() {
    assertThat(CatalogRequests.admin(admin)).isTrue();
    assertThat(CatalogRequests.admin(instructor)).isFalse();
    assertThat(CatalogRequests.actor(admin)).isEqualTo(actorId);
  }

  @Test
  void everyWriteRefusesANonAdminBeforeReadingTheBody() {
    // A null body would raise a NullPointerException if it were parsed first.
    rejects(() -> CatalogRequests.create(instructor, null), "INB-302");
    rejects(() -> CatalogRequests.update(instructor, CatalogKind.TOPIC, "x", null), "INB-302");
    rejects(() -> CatalogRequests.reorder(instructor, null), "INB-302");
    rejects(() -> CatalogRequests.delete(instructor, CatalogKind.LEVEL, "x"), "INB-302");
  }

  @Test
  void createReadsTheDisplayName() {
    CatalogCommand command = CatalogRequests.create(admin, json("{\"displayName\":\"Graphs\"}"));
    assertThat(command.displayName()).isEqualTo("Graphs");
    assertThat(command.admin()).isTrue();
    assertThat(command.id()).isNull();
    assertThat(CatalogRequests.create(admin, json("{}")).displayName()).isNull();
  }

  @Test
  void nonTextDisplayNameIsInvalid() {
    rejects(() -> CatalogRequests.create(admin, json("{\"displayName\":5}")), "INB-V03");
    rejects(
        () ->
            CatalogRequests.update(
                admin, CatalogKind.LEVEL, id.toString(), json("{\"displayName\":null}")),
        "INB-V03");
  }

  @Test
  void topicUpdateAcceptsNameStarOrBoth() {
    var nameOnly =
        CatalogRequests.update(
            admin, CatalogKind.TOPIC, id.toString(), json("{\"displayName\":\"N\"}"));
    assertThat(nameOnly.displayName()).isEqualTo("N");
    assertThat(nameOnly.usesStarFramework()).isNull();
    assertThat(nameOnly.id()).isEqualTo(id);
    var starOnly =
        CatalogRequests.update(
            admin, CatalogKind.TOPIC, id.toString(), json("{\"usesStarFramework\":true}"));
    assertThat(starOnly.usesStarFramework()).isTrue();
    assertThat(starOnly.displayName()).isNull();
  }

  @Test
  void topicUpdateRejectsEmptyBodyAndNonBooleanStar() {
    rejects(
        () -> CatalogRequests.update(admin, CatalogKind.TOPIC, id.toString(), json("{}")),
        "INB-V13");
    rejects(
        () ->
            CatalogRequests.update(
                admin, CatalogKind.TOPIC, id.toString(), json("{\"usesStarFramework\":\"yes\"}")),
        "INB-V13");
  }

  @Test
  void levelUpdateIgnoresStarFlagAndAllowsMissingName() {
    var command =
        CatalogRequests.update(
            admin, CatalogKind.LEVEL, id.toString(), json("{\"usesStarFramework\":true}"));
    assertThat(command.usesStarFramework()).isNull();
    assertThat(command.displayName()).isNull();
  }

  @Test
  void identifierThatIsNotAUuidMapsToKindSpecificNotFound() {
    assertThatThrownBy(() -> CatalogRequests.delete(admin, CatalogKind.TOPIC, "bad"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-011"));
    assertThatThrownBy(
            () ->
                CatalogRequests.update(
                    admin, CatalogKind.LEVEL, "bad", json("{\"displayName\":\"x\"}")))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-012"));
    assertThat(CatalogRequests.delete(admin, CatalogKind.TOPIC, id.toString()).id()).isEqualTo(id);
  }

  @Test
  void reorderParsesAnOrderedIdList() {
    UUID other = UUID.randomUUID();
    var command =
        CatalogRequests.reorder(admin, json("{\"orderedIds\":[\"" + other + "\",\"" + id + "\"]}"));
    assertThat(command.orderedIds()).containsExactly(other, id);
  }

  @Test
  void reorderRejectsMalformedLists() {
    rejects(() -> CatalogRequests.reorder(admin, json("{}")), "INB-V14");
    rejects(() -> CatalogRequests.reorder(admin, json("{\"orderedIds\":[1]}")), "INB-V14");
    rejects(() -> CatalogRequests.reorder(admin, json("{\"orderedIds\":[\"bad\"]}")), "INB-V14");
  }
}
