package com.algoprep.identity.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ServiceUnavailableException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Every storage failure is 503 IDT-511 without source detail; a missing object is 404 IDT-451. */
class MinioAvatarObjectStoreTest {
  private final MinioClient client = mock(MinioClient.class);
  private final MinioAvatarObjectStore store = new MinioAvatarObjectStore(() -> client, "bucket");

  private static ErrorResponseException error(String code) {
    var body = mock(ErrorResponse.class);
    when(body.code()).thenReturn(code);
    var failure = mock(ErrorResponseException.class);
    when(failure.errorResponse()).thenReturn(body);
    return failure;
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void putAndDeleteCallTheClientWithTheConfiguredBucket() throws Exception {
    store.put("users/a/b.png", new byte[] {1, 2, 3}, "image/png");
    store.delete("users/a/b.png");

    verify(client).putObject(any(PutObjectArgs.class));
    verify(client).removeObject(any(RemoveObjectArgs.class));
  }

  @Test
  void anIoFailureOnPutOrDeleteIs503Idt511() throws Exception {
    doThrow(new IOException("secret endpoint")).when(client).putObject(any(PutObjectArgs.class));
    doThrow(new IOException("secret endpoint"))
        .when(client)
        .removeObject(any(RemoveObjectArgs.class));

    assertThatThrownBy(() -> store.put("k", new byte[1], "image/png"))
        .isInstanceOf(ServiceUnavailableException.class)
        .satisfies(e -> assertCode(e, "IDT-511"))
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain("secret"));
    assertThatThrownBy(() -> store.delete("k")).satisfies(e -> assertCode(e, "IDT-511"));
  }

  @Test
  void aMissingObjectOnReadIs404Idt451() throws Exception {
    for (String code : new String[] {"NoSuchKey", "NoSuchObject", "NotFound"}) {
      var failure = error(code);
      doThrow(failure).when(client).getObject(any(GetObjectArgs.class));
      assertThatThrownBy(() -> store.get("k")).satisfies(e -> assertCode(e, "IDT-451"));
    }
  }

  @Test
  void anyOtherReadFailureIs503Idt511() throws Exception {
    var failure = error("InternalError");
    doThrow(failure).when(client).getObject(any(GetObjectArgs.class));
    assertThatThrownBy(() -> store.get("k")).satisfies(e -> assertCode(e, "IDT-511"));

    doThrow(new IOException("down")).when(client).getObject(any(GetObjectArgs.class));
    assertThatThrownBy(() -> store.get("k")).satisfies(e -> assertCode(e, "IDT-511"));
  }
}
