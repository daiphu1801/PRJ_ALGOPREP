package com.algoprep.problembank.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/** Verifies the real SDK/HTTP stack does not silently add transport retries. */
class StorageHttpPolicyTest {
  @Test
  void realTransportRetriesReadOnceAndNeverRetriesWrite() throws Exception {
    var requests = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.incrementAndGet();
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });
    server.start();
    try (var context = configuredContext(server.getAddress().getPort())) {
      var store = context.getBean(TestcaseObjectStorePort.class);
      assertThatThrownBy(() -> store.head("problems/a/testcases/b/input.txt"))
          .isInstanceOf(TestcaseStorageUnavailableException.class);
      assertThat(requests).hasValue(2);
      requests.set(0);
      assertThatThrownBy(() -> store.put("problems/a/testcases/b/input.txt", new byte[0]))
          .isInstanceOf(TestcaseStorageUnavailableException.class);
      assertThat(requests).hasValue(1);
    } finally {
      server.stop(0);
    }
  }

  private AnnotationConfigApplicationContext configuredContext(int port) {
    var context = new AnnotationConfigApplicationContext();
    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "storage-test",
                Map.of(
                    "algoprep.problem-bank.storage.endpoint",
                    "http://127.0.0.1:" + port,
                    "algoprep.problem-bank.storage.access-key",
                    "test-user",
                    "algoprep.problem-bank.storage.secret-key",
                    "test-only-password")));
    context.register(TestcaseObjectStoreConfiguration.class);
    context.refresh();
    return context;
  }
}
