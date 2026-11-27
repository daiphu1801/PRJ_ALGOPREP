package com.algoprep.identity.infrastructure.restoration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.account.RestorationGrant;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** Restoration ticket store: single-use, 5-minute value, malformed values are treated as absent. */
@SuppressWarnings("unchecked")
class RedisRestorationTicketAdapterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);
  private final RedisRestorationTicketAdapter adapter;
  private final UUID user = UUID.randomUUID();
  private final Instant deactivated = Instant.parse("2026-05-01T00:00:00Z");
  private final Instant expires = Instant.parse("2026-05-01T00:05:00Z");

  RedisRestorationTicketAdapterTest() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(ops);
    adapter = new RedisRestorationTicketAdapter(provider);
  }

  @Test
  void saveStoresPipeSeparatedGrantAndRejectsDuplicates() {
    String value = user + "|" + deactivated + "|" + expires;
    when(ops.setIfAbsent("identity:restore:h", value, Duration.ofSeconds(300)))
        .thenReturn(true, false);
    adapter.save("h", new RestorationGrant(user, deactivated, expires));
    assertThatThrownBy(() -> adapter.save("h", new RestorationGrant(user, deactivated, expires)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void findParsesGrantAndIgnoresMalformedValues() {
    when(ops.get("identity:restore:ok")).thenReturn(user + "|" + deactivated + "|" + expires);
    assertThat(adapter.find("ok")).contains(new RestorationGrant(user, deactivated, expires));
    assertThat(adapter.find("missing")).isEmpty();
    when(ops.get("identity:restore:short")).thenReturn("a|b");
    assertThat(adapter.find("short")).isEmpty();
    when(ops.get("identity:restore:bad")).thenReturn("not-uuid|x|y");
    assertThat(adapter.find("bad")).isEmpty();
    when(ops.get("identity:restore:time")).thenReturn(user + "|nope|" + expires);
    assertThat(adapter.find("time")).isEmpty();
  }

  @Test
  void deleteRemovesTheKey() {
    adapter.delete("h");
    verify(redis).delete("identity:restore:h");
  }
}
