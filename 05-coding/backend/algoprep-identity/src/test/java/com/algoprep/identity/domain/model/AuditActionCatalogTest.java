package com.algoprep.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-19 mapping by prefix, with a fallback for actions nobody mapped yet. */
class AuditActionCatalogTest {

  @Test
  void actionsWrittenTodayMapToAuth() {
    for (String action :
        List.of(
            "PASSWORD_CHANGED",
            "EMAIL_CHANGED",
            "PASSWORD_RESET_REQUESTED",
            "PASSWORD_RESET_COMPLETED",
            "SELF_ACCOUNT_DELETE_REQUESTED",
            "SECURITY_TOKEN_REUSE_DETECTED")) {
      AuditActionInfo info = AuditActionCatalog.resolve(action);
      assertThat(info.category()).as(action).isEqualTo(AuditCategory.AUTH);
      assertThat(info.service()).isEqualTo("auth");
    }
  }

  @Test
  void futureActionsOfOtherSlicesLandInTheirGroups() {
    assertThat(AuditActionCatalog.resolve("ROLE_DELETED").category())
        .isEqualTo(AuditCategory.PERMISSION);
    assertThat(AuditActionCatalog.resolve("USER_CREATED").category())
        .isEqualTo(AuditCategory.PERMISSION);
    assertThat(AuditActionCatalog.resolve("AUDIT_LOG_SETTINGS_CHANGED").category())
        .isEqualTo(AuditCategory.CONFIG);
    assertThat(AuditActionCatalog.resolve("LOG_RETENTION_PURGED").category())
        .isEqualTo(AuditCategory.CONFIG);
    assertThat(AuditActionCatalog.resolve("PROBLEM_PUBLISHED").category())
        .isEqualTo(AuditCategory.CONTENT);
    assertThat(AuditActionCatalog.resolve("INTERVIEW_TOPIC_CREATED").category())
        .isEqualTo(AuditCategory.CONTENT);
  }

  @Test
  void aiConfigIsItsOwnServiceInsideConfigAndNotContent() {
    AuditActionInfo info = AuditActionCatalog.resolve("AI_TESTCASE_GENERATION_LIMIT_CHANGED");

    assertThat(info.category()).isEqualTo(AuditCategory.CONFIG);
    assertThat(info.service()).isEqualTo("ai-config");
  }

  @Test
  void anUnknownActionFallsBackToConfigSystem() {
    AuditActionInfo info = AuditActionCatalog.resolve("SOMETHING_NEW");

    assertThat(info).isEqualTo(new AuditActionInfo(AuditCategory.CONFIG, "system"));
  }

  @Test
  void noPrefixIsAPrefixOfAnotherSoRuleOrderCannotMatter() {
    List<String> all = AuditActionCatalog.ofCategory(AuditCategory.AUTH).knownPrefixes();

    for (String a : all) {
      for (String b : all) {
        assertThat(a.equals(b) || !a.startsWith(b)).as(a + " vs " + b).isTrue();
      }
    }
  }

  @Test
  void onlyConfigTakesUnmappedActions() {
    assertThat(AuditActionCatalog.ofCategory(AuditCategory.CONFIG).includeUnmapped()).isTrue();
    assertThat(AuditActionCatalog.ofCategory(AuditCategory.AUTH).includeUnmapped()).isFalse();
  }

  @Test
  void serviceKeywordMatchesByContainsIgnoringCaseAndMayMatchNothing() {
    assertThat(AuditActionCatalog.ofServiceContaining("AUTH").prefixes()).contains("PASSWORD_");
    assertThat(AuditActionCatalog.ofServiceContaining("config").prefixes())
        .contains("AI_", "QUEUE_");
    ActionMatch none = AuditActionCatalog.ofServiceContaining("zzz");
    assertThat(none.prefixes()).isEmpty();
    assertThat(none.includeUnmapped()).isFalse();
    assertThat(AuditActionCatalog.ofServiceContaining("sys").includeUnmapped()).isTrue();
  }
}
