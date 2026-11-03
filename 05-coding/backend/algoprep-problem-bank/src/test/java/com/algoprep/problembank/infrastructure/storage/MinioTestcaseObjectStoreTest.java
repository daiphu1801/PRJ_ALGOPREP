package com.algoprep.problembank.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class MinioTestcaseObjectStoreTest {
  private static final String KEY = "problems/a/testcases/b/input.txt";
  private static final String BUCKET = "algoprep-testcases";
  private final MinioClient service = mock(MinioClient.class);
  private final MinioClient signer = mock(MinioClient.class);
  private final MinioTestcaseObjectStore store =
      new MinioTestcaseObjectStore(() -> service, () -> signer, BUCKET);

  @Test
  void signsWithThePublicClientAndExplicitExpiry() throws Exception {
    when(signer.getPresignedObjectUrl(any())).thenReturn("signed");
    assertThat(store.presignGet(KEY, Duration.ofMinutes(5))).isEqualTo("signed");
    assertThat(store.presignPut(KEY, Duration.ofMinutes(15))).isEqualTo("signed");
    var args = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
    verify(signer, times(2)).getPresignedObjectUrl(args.capture());
    assertThat(args.getAllValues()).extracting(a -> a.expiry()).containsExactly(300, 900);
    assertThat(args.getAllValues())
        .extracting(a -> a.method().name())
        .containsExactly("GET", "PUT");
    assertThat(args.getAllValues())
        .allMatch(a -> a.bucket().equals(BUCKET) && a.object().equals(KEY));
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        "/problems/x",
        "other/x",
        "problems/../x",
        "problems/./x",
        "problems//x",
        "problems/x/",
        "problems/x\\y",
        "problems/x\ny"
      })
  void rejectsUnsafeKeysBeforeCallingSdk(String key) {
    assertThatThrownBy(() -> store.head(key)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(service, signer);
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 301, Long.MAX_VALUE})
  void rejectsGetExpiryOutsideAllowedRange(long seconds) {
    assertThatThrownBy(() -> store.presignGet(KEY, Duration.ofSeconds(seconds)))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(service, signer);
  }

  @Test
  void rejectsNullFractionalAndTooLongUploadExpiry() {
    assertThatThrownBy(() -> store.presignGet(KEY, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.presignGet(KEY, Duration.ofMillis(1500)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.presignPut(KEY, Duration.ofSeconds(901)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> store.put(KEY, null)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(service, signer);
  }

  @Test
  void retriesHeadOnceAndKeepsByteSize() throws Exception {
    var stat = mock(StatObjectResponse.class);
    when(stat.size()).thenReturn(12345L);
    when(service.statObject(any())).thenThrow(new IOException("private")).thenReturn(stat);
    assertThat(store.head(KEY)).isEqualTo(12345);
    var args = ArgumentCaptor.forClass(StatObjectArgs.class);
    verify(service, times(2)).statObject(args.capture());
    assertThat(args.getValue().bucket()).isEqualTo(BUCKET);
    assertThat(args.getValue().object()).isEqualTo(KEY);
  }

  @Test
  void retriesWholeGetAndClosesBothResponses() throws Exception {
    var first = mock(GetObjectResponse.class);
    var second = mock(GetObjectResponse.class);
    var bytes = "đầu vào\n".getBytes(StandardCharsets.UTF_8);
    when(first.readAllBytes()).thenThrow(new IOException("private"));
    when(second.readAllBytes()).thenReturn(bytes);
    when(service.getObject(any())).thenReturn(first, second);
    assertThat(store.get(KEY)).containsExactly(bytes);
    var args = ArgumentCaptor.forClass(GetObjectArgs.class);
    verify(service, times(2)).getObject(args.capture());
    assertThat(args.getValue().object()).isEqualTo(KEY);
    verify(first).close();
    verify(second).close();
  }

  @Test
  void readFailureIsRetriedExactlyOnceAndRedacted() throws Exception {
    when(service.statObject(any())).thenThrow(new IOException("signed-url/private-hidden-body"));
    assertUnavailable(() -> store.head(KEY));
    verify(service, times(2)).statObject(any());
  }

  @Test
  void writesAreNotRetried() throws Exception {
    when(service.putObject(any())).thenThrow(new IOException("private"));
    when(service.copyObject(any())).thenThrow(new IOException("private"));
    org.mockito.Mockito.doThrow(new IOException("private")).when(service).removeObject(any());
    assertUnavailable(() -> store.put(KEY, new byte[0]));
    assertUnavailable(() -> store.copy(KEY, "problems/c/testcases/d/input.txt"));
    assertUnavailable(() -> store.delete(KEY));
    verify(service).putObject(any());
    verify(service).copyObject(any());
    verify(service).removeObject(any());
  }

  @Test
  void interruptionStopsReadRetryAndRetainsFlag() throws Exception {
    when(service.statObject(any()))
        .thenAnswer(
            invocation -> {
              Thread.currentThread().interrupt();
              throw new IOException("private");
            });
    try {
      assertUnavailable(() -> store.head(KEY));
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verify(service).statObject(any());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void clientCreationFailureIsRedactedAndMissingCredentialsDoNotLeak() {
    var calls = new AtomicInteger();
    var unavailable =
        new MinioTestcaseObjectStore(
            () -> {
              calls.incrementAndGet();
              throw new IllegalStateException("credential-private");
            },
            () -> signer,
            BUCKET);
    assertUnavailable(() -> unavailable.get(KEY));
    assertThat(calls).hasValue(2);
  }

  @Test
  void signingFailureIsRedactedWithoutNetworkRetry() throws Exception {
    when(signer.getPresignedObjectUrl(any())).thenThrow(new IOException("private"));
    assertUnavailable(() -> store.presignGet(KEY, Duration.ofSeconds(1)));
    verify(signer).getPresignedObjectUrl(any());
    verify(service, never()).getObject(any());
  }

  @Test
  void usesServerSideCopyAndSingleBucket() throws Exception {
    String target = "problems/new/testcases/new/expected_output.txt";
    store.copy(KEY, target);
    var args = ArgumentCaptor.forClass(CopyObjectArgs.class);
    verify(service).copyObject(args.capture());
    assertThat(args.getValue().bucket()).isEqualTo(BUCKET);
    assertThat(args.getValue().object()).isEqualTo(target);
    assertThat(args.getValue().source().object()).isEqualTo(KEY);
    assertThat(args.getValue().source().bucket()).isEqualTo(BUCKET);
    store.delete(target);
    var removal = ArgumentCaptor.forClass(RemoveObjectArgs.class);
    verify(service).removeObject(removal.capture());
    assertThat(removal.getValue().object()).isEqualTo(target);
  }

  @Test
  void putsExactBytesIncludingEmptyPayload() throws Exception {
    byte[] bytes = "dữ liệu\n".getBytes(StandardCharsets.UTF_8);
    when(service.putObject(any()))
        .thenAnswer(
            invocation -> {
              PutObjectArgs args = invocation.getArgument(0);
              assertThat(args.stream().readAllBytes()).containsExactly(bytes);
              assertThat(args.objectSize()).isEqualTo(bytes.length);
              assertThat(args.bucket()).isEqualTo(BUCKET);
              assertThat(args.object()).isEqualTo(KEY);
              return null;
            });
    store.put(KEY, bytes);
  }

  @Test
  void wholeOperationDeadlineCancelsHungWorkersAndDoesNotRetryWrites() throws Exception {
    var started = new CountDownLatch(1);
    var stopped = new CountDownLatch(1);
    when(service.putObject(any()))
        .thenAnswer(
            invocation -> {
              started.countDown();
              try {
                new CountDownLatch(1).await();
              } finally {
                stopped.countDown();
              }
              return null;
            });
    var bounded =
        new MinioTestcaseObjectStore(() -> service, () -> signer, BUCKET, Duration.ofMillis(250));
    assertUnavailable(() -> bounded.put(KEY, new byte[0]));
    assertThat(started.getCount()).isZero();
    assertThat(stopped.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    verify(service).putObject(any());
  }

  private static void assertUnavailable(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            TestcaseStorageUnavailableException.class,
            failure -> {
              assertThat(failure.errorCode()).isEqualTo("PRB-052");
              assertThat(failure.getMessage()).isEqualTo("Testcase storage is unavailable");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
