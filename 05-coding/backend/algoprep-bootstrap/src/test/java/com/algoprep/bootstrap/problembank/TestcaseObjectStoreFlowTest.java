package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.infrastructure.storage.TestcaseObjectStoreConfiguration;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Only the actual storage configuration plus an isolated private MinIO, no database stand-ins. */
@SpringJUnitConfig(TestcaseObjectStoreConfiguration.class)
@Testcontainers
class TestcaseObjectStoreFlowTest {
  private static final String BUCKET = "algoprep-testcases";

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-storage-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-storage-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add(
        "algoprep.problem-bank.storage.public-endpoint",
        () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-storage-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-storage-password");
  }

  @Autowired TestcaseObjectStorePort storage;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient client;

  @BeforeEach
  void privateBucket() throws Exception {
    if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
  }

  private String key() {
    return "problems/" + UUID.randomUUID() + "/testcases/" + UUID.randomUUID() + "/input.txt";
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 8192, 8193, 65536})
  void storesAndReadsExactByteSizesIncludingEmptyObject(int size) {
    String key = key();
    byte[] bytes = new byte[size];
    new java.util.Random(42).nextBytes(bytes);
    storage.put(key, bytes);
    assertThat(storage.head(key)).isEqualTo(size);
    assertThat(storage.get(key)).containsExactly(bytes);
  }

  @Test
  void roundTripsUtf8AndCopyIsIndependentOfLaterSourceChanges() {
    String source = key();
    String destination = key();
    byte[] original = "tiếng Việt\n\u0000\n".getBytes(StandardCharsets.UTF_8);
    storage.put(source, original);
    storage.copy(source, destination);
    storage.put(source, "changed".getBytes(StandardCharsets.UTF_8));
    assertThat(storage.get(destination)).containsExactly(original);
    storage.delete(source);
    storage.delete(source);
    assertThat(storage.get(destination)).containsExactly(original);
    assertThatThrownBy(() -> storage.head(source))
        .isInstanceOf(TestcaseStorageUnavailableException.class);
  }

  @Test
  void signedPutAndGetWorkWhileAnonymousDownloadsAreDenied() throws Exception {
    String key = key();
    byte[] bytes = "ẩn/private\n".getBytes(StandardCharsets.UTF_8);
    URI putUrl = URI.create(storage.presignPut(key, Duration.ofMinutes(15)));
    URI getUrl = URI.create(storage.presignGet(key, Duration.ofMinutes(5)));
    assertThat(putUrl.getHost()).isEqualTo("127.0.0.1");
    assertThat(getUrl.getQuery()).contains("X-Amz-Expires=300");
    assertThat(putUrl.getQuery()).contains("X-Amz-Expires=900");
    try (var http = HttpClient.newHttpClient()) {
      var put =
          HttpRequest.newBuilder(putUrl).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
      assertThat(http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode())
          .isEqualTo(200);
      var get = HttpRequest.newBuilder(getUrl).GET().build();
      var response = http.send(get, HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).containsExactly(bytes);
      URI anonymous =
          new URI(getUrl.getScheme(), getUrl.getAuthority(), getUrl.getPath(), null, null);
      assertThat(
              http.send(
                      HttpRequest.newBuilder(anonymous).GET().build(),
                      HttpResponse.BodyHandlers.discarding())
                  .statusCode())
          .isEqualTo(403);
    }
    assertThat(storage.get(key)).containsExactly(bytes);
  }

  @Test
  void signedUrlCannotBeReusedForADifferentObjectOrMethod() throws Exception {
    String key = key();
    storage.put(key, new byte[] {1});
    URI signed = URI.create(storage.presignGet(key, Duration.ofMinutes(5)));
    URI tampered = URI.create(signed.toString().replace("/input.txt?", "/expected_output.txt?"));
    try (var http = HttpClient.newHttpClient()) {
      var get = HttpRequest.newBuilder(tampered).GET().build();
      assertThat(http.send(get, HttpResponse.BodyHandlers.discarding()).statusCode())
          .isEqualTo(403);
      var put = HttpRequest.newBuilder(signed).PUT(HttpRequest.BodyPublishers.noBody()).build();
      assertThat(http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode())
          .isEqualTo(403);
    }
    assertThat(storage.get(key)).containsExactly((byte) 1);
  }

  @Test
  void missingObjectsAreSanitizedForBothGetAndHead() {
    String key = key();
    assertMissing(() -> storage.get(key));
    assertMissing(() -> storage.head(key));
  }

  private void assertMissing(Runnable read) {
    assertThatThrownBy(read::run)
        .isInstanceOfSatisfying(
            TestcaseStorageUnavailableException.class,
            failure -> {
              assertThat(failure.errorCode()).isEqualTo("PRB-052");
              assertThat(failure.getMessage()).isEqualTo("Testcase storage is unavailable");
              assertThat(failure.getCause()).isNull();
            });
  }
}
