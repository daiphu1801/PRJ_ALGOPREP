package com.algoprep.problembank.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import io.minio.MinioClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class TestcaseObjectStoreConfigurationTest {
  @Test
  void missingCredentialsDoNotBreakStartupButStorageFailsClosed() {
    try (var context = context(Map.of())) {
      var store = context.getBean(TestcaseObjectStorePort.class);
      assertThat(context.getBeanFactory().containsSingleton("testcaseServiceClient")).isFalse();
      assertThat(context.getBeanFactory().containsSingleton("testcaseSigningClient")).isFalse();
      assertThatThrownBy(() -> store.head("problems/a/testcases/b/input.txt"))
          .isInstanceOfSatisfying(
              TestcaseStorageUnavailableException.class,
              failure -> assertThat(failure.getCause()).isNull());
    }
  }

  @Test
  void publicSigningDoesNotResolveInternalHostOrQueryStorage() {
    try (var context =
        context(
            Map.of(
                "algoprep.problem-bank.storage.endpoint", "http://internal-storage.invalid:9000",
                "algoprep.problem-bank.storage.public-endpoint", "https://storage.example.test",
                "algoprep.problem-bank.storage.access-key", "test-user",
                "algoprep.problem-bank.storage.secret-key", "test-only-password"))) {
      String url =
          context
              .getBean(TestcaseObjectStorePort.class)
              .presignGet("problems/a/testcases/b/input.txt", Duration.ofMinutes(5));
      assertThat(java.net.URI.create(url).getHost()).isEqualTo("storage.example.test");
      assertThat(context.getBeanFactory().containsSingleton("testcaseServiceClient")).isFalse();
      assertThat(context.getBean("testcaseSigningClient")).isInstanceOf(MinioClient.class);
    }
  }

  private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
    var context = new AnnotationConfigApplicationContext();
    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(new MapPropertySource("storage-test", properties));
    context.register(TestcaseObjectStoreConfiguration.class);
    context.refresh();
    return context;
  }
}
