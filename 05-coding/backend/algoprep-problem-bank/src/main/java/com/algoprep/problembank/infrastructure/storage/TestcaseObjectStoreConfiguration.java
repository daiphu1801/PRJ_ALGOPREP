package com.algoprep.problembank.infrastructure.storage;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import io.minio.MinioClient;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Lazy credentials keep unrelated flows independent of storage availability. No bucket mutation.
 */
@Configuration(proxyBeanMethods = false)
public class TestcaseObjectStoreConfiguration {
  @Bean(destroyMethod = "close")
  @Lazy
  MinioClient testcaseServiceClient(
      @Value("${algoprep.problem-bank.storage.endpoint:http://localhost:9000}") String endpoint,
      @Value("${algoprep.problem-bank.storage.access-key:}") String access,
      @Value("${algoprep.problem-bank.storage.secret-key:}") String secret) {
    return client(endpoint, access, secret);
  }

  @Bean(destroyMethod = "close")
  @Lazy
  MinioClient testcaseSigningClient(
      @Value(
              "${algoprep.problem-bank.storage.public-endpoint:${algoprep.problem-bank.storage.endpoint:http://localhost:9000}}")
          String endpoint,
      @Value("${algoprep.problem-bank.storage.access-key:}") String access,
      @Value("${algoprep.problem-bank.storage.secret-key:}") String secret) {
    return client(endpoint, access, secret);
  }

  @Bean
  TestcaseObjectStorePort testcaseObjectStore(
      @Qualifier("testcaseServiceClient") ObjectProvider<MinioClient> service,
      @Qualifier("testcaseSigningClient") ObjectProvider<MinioClient> signing,
      @Value("${algoprep.problem-bank.storage.bucket:algoprep-testcases}") String bucket) {
    return new MinioTestcaseObjectStore(service::getObject, signing::getObject, bucket);
  }

  private MinioClient client(String endpoint, String access, String secret) {
    if (access.isBlank() || secret.isBlank()) {
      throw new IllegalArgumentException("Testcase storage credentials are not configured");
    }
    var http =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(10))
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .retryOnConnectionFailure(false)
            .build();
    return MinioClient.builder()
        .endpoint(endpoint)
        .region("us-east-1")
        .credentials(access, secret)
        .httpClient(http, true)
        .build();
  }
}
