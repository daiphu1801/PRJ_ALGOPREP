package com.algoprep.problembank.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestcaseStorageClassificationTest {
  private final MinioClient client = mock(MinioClient.class);
  private final MinioTestcaseObjectStore storage =
      new MinioTestcaseObjectStore(() -> client, () -> client, "algoprep-testcases");

  @ParameterizedTest
  @ValueSource(
      strings = {
        "NoSuchKey",
        "NoSuchObject",
        "NotFound",
        "AccessDenied",
        "NoSuchBucket",
        "InternalError"
      })
  void onlyMissingObjectCodesAreDistinguishedWithoutRetainingSdkDetails(String code)
      throws Exception {
    var response = mock(ErrorResponse.class);
    when(response.code()).thenReturn(code);
    var error = mock(ErrorResponseException.class);
    when(error.errorResponse()).thenReturn(response);
    when(client.statObject(any(StatObjectArgs.class))).thenThrow(error);
    assertFailure(java.util.Set.of("NoSuchKey", "NoSuchObject", "NotFound").contains(code));
  }

  @Test
  void transportAndMalformedSdkErrorsRemainInfrastructureFailures() throws Exception {
    var response = mock(ErrorResponse.class);
    var error = mock(ErrorResponseException.class);
    when(error.errorResponse()).thenReturn(response);
    when(client.statObject(any(StatObjectArgs.class)))
        .thenThrow(error)
        .thenThrow(error)
        .thenThrow(new java.io.IOException("private credentials"));
    assertFailure(false);
    assertFailure(false);
  }

  private void assertFailure(boolean missing) {
    assertThatThrownBy(() -> storage.head("problems/example/snapshot.json"))
        .isInstanceOfSatisfying(
            TestcaseStorageUnavailableException.class,
            failure -> {
              assertThat(failure.isObjectMissing()).isEqualTo(missing);
              assertThat(failure.errorCode()).isEqualTo("PRB-052");
              assertThat(failure.getMessage()).isEqualTo("Testcase storage is unavailable");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
