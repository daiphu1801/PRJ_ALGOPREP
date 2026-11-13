package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CatalogReadCacheTest {
  @SuppressWarnings("unchecked")
  private final ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);

  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> values = mock(ValueOperations.class);

  private final CatalogReadCache cache = new CatalogReadCache(provider, mock(ObjectProvider.class));

  @BeforeEach
  void setup() {
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(values);
  }

  @Test
  void hitSkipsOriginAndRecordsRoundTripCorrectly() {
    when(values.get("key")).thenReturn("{\"items\":[],\"totalItems\":0,\"page\":1}");
    assertThat(
            cache.read(
                "key",
                Page.class,
                () -> {
                  throw new AssertionError("origin called");
                }))
        .isEqualTo(new Page(List.of(), 0, 1));
  }

  @Test
  void missStoresOnlySuccessfulOriginWithSixtySecondTtl() {
    Page page = new Page(List.of(), 0, 1);
    assertThat(cache.read("key", Page.class, () -> page)).isSameAs(page);
    verify(values).set(eq("key"), anyString(), eq(Duration.ofSeconds(60)));
  }

  @Test
  void malformedPayloadAndRedisOutageFallBackButOriginFailureEscapes() {
    when(values.get("key")).thenReturn("bad-json");
    assertThat(cache.read("key", Page.class, () -> new Page(List.of(), 0, 1))).isNotNull();
    when(values.get("key")).thenThrow(new IllegalStateException("redis down"));
    assertThat(cache.read("key", Page.class, () -> new Page(List.of(), 0, 1))).isNotNull();
    var failure = new IllegalStateException("database down");
    assertThatThrownBy(
            () ->
                cache.read(
                    "key",
                    Page.class,
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);
  }

  @Test
  void structurallyInvalidJsonDoesNotBecomeASuccessfulCacheHit() {
    when(values.get("key")).thenReturn("{\"items\":null,\"totalItems\":0,\"page\":0}");
    var page = new Page(List.of(), 0, 1);
    assertThat(cache.read("key", Page.class, () -> page)).isSameAs(page);
    when(values.get("states")).thenReturn("{\"solved\":null,\"attempted\":null}");
    assertThat(cache.read("states", SolveStates.class, () -> null)).isNull();
  }

  @Test
  void absentOriginStateIsNeverStored() {
    assertThat(cache.read("key", SolveStates.class, () -> null)).isNull();
    verify(values, never()).set(anyString(), anyString(), any(Duration.class));
  }
}
