package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BR-16: sensitive keys are replaced by "***" at any depth, structure is kept. */
class AuditLogMaskerTest {

  @Test
  void keysMatchingPasswordSecretOrTokenAreMaskedCaseInsensitively() {
    String after =
        "{\"password_hash\":\"$2a$x\",\"token_hash\":\"abc\",\"ApiSecret\":\"s\","
            + "\"newPassword\":\"p\",\"role\":\"ADMIN\"}";

    Map<String, Object> params = AuditLogMasker.safeParams(null, after);

    assertThat(params)
        .containsEntry("password_hash", "***")
        .containsEntry("token_hash", "***")
        .containsEntry("ApiSecret", "***")
        .containsEntry("newPassword", "***")
        .containsEntry("role", "ADMIN");
  }

  @Test
  void nestedObjectsAndArraysAreMaskedAndTheRestOfTheStructureStays() {
    String after =
        "{\"a\":{\"refreshToken\":\"t\",\"keep\":1},\"list\":[{\"secret\":\"x\",\"n\":2}]}";

    Map<String, Object> params = AuditLogMasker.safeParams(null, after);

    assertThat(params.get("a")).isEqualTo(Map.of("refreshToken", "***", "keep", 1));
    assertThat(params.get("list")).isEqualTo(List.of(Map.of("secret", "***", "n", 2)));
  }

  @Test
  void aMaskedValueNeverContainsTheOriginalSecret() {
    String after = "{\"token\":\"super-secret-value\"}";

    assertThat(AuditLogMasker.safeParams(null, after).toString())
        .doesNotContain("super-secret-value");
  }

  @Test
  void beforeJsonIsUsedWhenThereIsNoAfterSnapshotAndItIsMaskedToo() {
    String before = "{\"id\":\"r1\",\"name\":\"Role\",\"passwordHint\":\"x\"}";

    assertThat(AuditLogMasker.safeParams(before, null))
        .containsEntry("id", "r1")
        .containsEntry("passwordHint", "***");
  }

  @Test
  void tokenCounterAndBudgetKeysStayVisible() {
    String after =
        "{\"maxTokens\":4000,\"total_tokens\":12,\"token_budget\":900,\"tokenLimit\":5,"
            + "\"promptTokens\":3,\"remaining-tokens\":7}";

    Map<String, Object> params = AuditLogMasker.safeParams(null, after);

    assertThat(params)
        .containsEntry("maxTokens", 4000)
        .containsEntry("total_tokens", 12)
        .containsEntry("token_budget", 900)
        .containsEntry("tokenLimit", 5)
        .containsEntry("promptTokens", 3)
        .containsEntry("remaining-tokens", 7);
  }

  @Test
  void theExceptionNeverUncoversRealSecrets() {
    String after =
        "{\"accessToken\":\"a\",\"token\":\"b\",\"refresh_token\":\"c\",\"tokenHash\":\"d\","
            + "\"passwordTokensUsed\":\"e\",\"secretMaxTokens\":\"f\",\"tokensOwner\":\"g\"}";

    Map<String, Object> params = AuditLogMasker.safeParams(null, after);

    assertThat(params.values()).containsOnly("***");
    assertThat(params).hasSize(7);
  }

  @Test
  void afterJsonWinsOverBeforeJson() {
    assertThat(AuditLogMasker.safeParams("{\"v\":1}", "{\"v\":2}")).containsEntry("v", 2);
  }

  @Test
  void missingBlankOrNonObjectSnapshotsGiveNoParams() {
    assertThat(AuditLogMasker.safeParams(null, null)).isEmpty();
    assertThat(AuditLogMasker.safeParams(" ", "")).isEmpty();
    assertThat(AuditLogMasker.safeParams("[1,2]", null)).isEmpty();
    assertThat(AuditLogMasker.safeParams(null, "not json")).isEmpty();
  }
}
