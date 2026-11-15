package com.algoprep.identity.infrastructure.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.dto.account.EmailChangeIssueResult;
import com.algoprep.identity.domain.model.password.PasswordResetChallenge;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/** OTP stores: the result codes of the Lua scripts map to the port contract. */
@SuppressWarnings({"unchecked", "rawtypes"})
class RedisOtpAdaptersTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);
  private final ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
  private final UUID user = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");

  RedisOtpAdaptersTest() {
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
  }

  @Test
  void emailVerificationIssueMatchClear() {
    var adapter = new RedisEmailVerificationOtpAdapter(provider);
    when(redis.execute(any(RedisScript.class), anyList(), any()))
        .thenReturn(1L, 0L, null, 1L, 0L, 1L);
    assertThat(adapter.issue(user, "d")).isTrue();
    assertThat(adapter.issue(user, "d")).isFalse();
    assertThat(adapter.issue(user, "d")).isFalse();
    assertThat(adapter.matches(user, "d")).isTrue();
    assertThat(adapter.matches(user, "x")).isFalse();
    adapter.clear(user, "d");
    verify(redis, org.mockito.Mockito.times(4))
        .execute(
            any(RedisScript.class),
            eq(List.of("identity:emailverify:" + user, "identity:emailverify:resend:" + user)),
            eq("d"));
  }

  @Test
  void emailChangeIssueMapsEveryScriptResult() {
    var adapter = new RedisEmailChangeOtpAdapter(provider);
    when(redis.execute(any(RedisScript.class), anyList(), any(), any()))
        .thenReturn(0L, 1L, 2L, 9L, null);
    assertThat(adapter.issue(user, "n@b.c", "d")).isEqualTo(EmailChangeIssueResult.ISSUED);
    assertThat(adapter.issue(user, "n@b.c", "d")).isEqualTo(EmailChangeIssueResult.COOLDOWN);
    assertThat(adapter.issue(user, "n@b.c", "d")).isEqualTo(EmailChangeIssueResult.BUDGET_EXCEEDED);
    assertThatThrownBy(() -> adapter.issue(user, "n@b.c", "d"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> adapter.issue(user, "n@b.c", "d"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void emailChangeMatchReturnsPendingAddressOrEmpty() {
    var adapter = new RedisEmailChangeOtpAdapter(provider);
    when(redis.execute(any(RedisScript.class), anyList(), any()))
        .thenReturn("n@b.c", (String) null);
    assertThat(adapter.match(user, "d")).contains("n@b.c");
    assertThat(adapter.match(user, "d")).isEmpty();
    adapter.clear(user, "d");
    verify(redis)
        .execute(
            any(RedisScript.class),
            eq(List.of("identity:emailchange:" + user, "identity:emailchange:resend:" + user)),
            eq("d"));
  }

  @Test
  void passwordResetIssueVerifyGuidanceAndClear() {
    var adapter = new RedisPasswordResetOtpAdapter(provider);
    when(redis.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L, 0L);
    assertThat(adapter.issue(user, new PasswordResetChallenge("d", now))).isTrue();
    assertThat(adapter.issue(user, new PasswordResetChallenge("d", now))).isFalse();

    when(redis.execute(any(RedisScript.class), anyList(), any(String.class)))
        .thenReturn(now.toString(), "", null);
    assertThat(adapter.verify(user, "d")).contains(now);
    assertThat(adapter.verify(user, "d")).isEmpty();
    assertThat(adapter.verify(user, "d")).isEmpty();

    when(ops.setIfAbsent("identity:pwreset:resend:" + user, now.toString(), Duration.ofSeconds(60)))
        .thenReturn(true, false);
    assertThat(adapter.reserveGuidance(user, now)).isTrue();
    assertThat(adapter.reserveGuidance(user, now)).isFalse();
    adapter.clear(user, now);
    verify(redis, org.mockito.Mockito.atLeastOnce())
        .execute(
            any(RedisScript.class),
            eq(List.of("identity:pwreset:" + user, "identity:pwreset:resend:" + user)),
            eq(now.toString()));
  }
}
