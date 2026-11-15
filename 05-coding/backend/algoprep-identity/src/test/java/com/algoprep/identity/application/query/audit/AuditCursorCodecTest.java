package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.domain.model.AuditCategory;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The #39 cursor: round trip, bound to its filter, anything else is IDT-443. */
class AuditCursorCodecTest {
  private final AuditLogFilter filter = new AuditLogFilter("abc", AuditCategory.AUTH, null, null);
  private final AuditLogCursor cursor =
      new AuditLogCursor(Instant.parse("2026-11-01T03:04:05.123456Z"), UUID.randomUUID());

  @Test
  void aCursorRoundTripsWithMicrosecondPrecision() {
    String token = AuditCursorCodec.encode(cursor, filter);

    assertThat(AuditCursorCodec.decode(token, filter)).isEqualTo(cursor);
  }

  @Test
  void theTokenIsOpaqueAndUrlSafe() {
    String token = AuditCursorCodec.encode(cursor, filter);

    assertThat(token).matches("[A-Za-z0-9_-]+").doesNotContain(cursor.id().toString());
  }

  @Test
  void aCursorReusedUnderAnotherFilterIsRejected() {
    String token = AuditCursorCodec.encode(cursor, filter);

    assertThatThrownBy(() -> AuditCursorCodec.decode(token, AuditLogFilter.none()))
        .satisfies(e -> assertCode(e));
  }

  @Test
  void garbageIsRejected() {
    for (String bad : new String[] {"!!!", "", "YQ", "MXwyfDM"}) {
      assertThatThrownBy(() -> AuditCursorCodec.decode(bad, filter)).satisfies(e -> assertCode(e));
    }
  }

  private static void assertCode(Throwable error) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-443"));
  }
}
