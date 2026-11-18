package com.algoprep.identity.infrastructure.storage;

import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
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
 * Lazy client: the application starts and F1 flows without avatars work even when MinIO is down or
 * unconfigured; only an avatar call then fails with 503 IDT-511. No bucket is created or changed.
 */
@Configuration(proxyBeanMethods = false)
public class AvatarObjectStoreConfiguration {

  @Bean(destroyMethod = "close")
  @Lazy
  MinioClient avatarMinioClient(
      @Value("${algoprep.identity.avatar.storage.endpoint:http://localhost:9000}") String endpoint,
      @Value("${algoprep.identity.avatar.storage.access-key:}") String access,
      @Value("${algoprep.identity.avatar.storage.secret-key:}") String secret) {
    if (access.isBlank() || secret.isBlank()) {
      throw new IllegalArgumentException("Avatar storage credentials are not configured");
    }
    var http =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(10))
            .connectTimeout(Duration.ofSeconds(5))
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

  @Bean
  AvatarObjectStorePort avatarObjectStore(
      @Qualifier("avatarMinioClient") ObjectProvider<MinioClient> client,
      @Value("${algoprep.identity.avatar.storage.bucket:algoprep-avatars}") String bucket) {
    return new MinioAvatarObjectStore(client::getObject, bucket);
  }
}
