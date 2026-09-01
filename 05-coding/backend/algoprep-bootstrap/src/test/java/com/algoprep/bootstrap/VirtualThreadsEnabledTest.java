package com.algoprep.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Request THẬT phải được xử lý trên virtual thread.
 *
 * <p>Vì sao test này tồn tại thay vì tin vào một dòng YAML: bỏ {@code
 * spring.threads.virtual.enabled} thì ứng dụng vẫn chạy đúng, {@code mvnw verify} vẫn xanh, và toàn
 * bộ {@code DEC-2026-0820-stack-versions} (chọn Java 21 thay vì 17 CHỈ vì Virtual Threads) bị vô
 * hiệu trong im lặng cho tới lúc chạy k6 và không hiểu vì sao số không đẹp.
 *
 * <p>Cách kiểm: cài một {@link Filter} chỉ có trong test, ghi lại {@code Thread.isVirtual()} của
 * thread đang xử lý, rồi gọi thật một endpoint qua HTTP. Không thêm endpoint nào vào mã sản phẩm
 * chỉ để phục vụ test.
 *
 * <p>Client dùng {@link HttpClient} của JDK, không dùng tiện ích test HTTP của framework: nó có sẵn
 * trong JDK 21, không thêm dependency, và không phụ thuộc vào việc lớp tiện ích đó nằm ở artifact
 * nào — {@code TestRestTemplate} đã bị dời khỏi {@code spring-boot-test} ở Spring Boot 4.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadsEnabledTest.ThreadProbeConfig.class)
@ActiveProfiles("test")
class VirtualThreadsEnabledTest {

  @LocalServerPort private int port;

  @Autowired private ThreadProbeConfig probe;

  @Test
  @DisplayName("thread xử lý request là virtual thread")
  void requestChayTrenVirtualThread() throws IOException, InterruptedException {
    goiActuatorHealth();

    assertThat(probe.wasCalled.get())
        .as("filter phải được gọi, nếu không thì phép đo bên dưới vô nghĩa")
        .isTrue();
    assertThat(probe.wasVirtual.get())
        .as("spring.threads.virtual.enabled đang tắt hoặc bị ghi đè ở đâu đó")
        .isTrue();
  }

  private void goiActuatorHealth() throws IOException, InterruptedException {
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode())
          .as("/actuator/health phải mở công khai theo SecurityConfig")
          .isEqualTo(200);
    }
  }

  @TestConfiguration
  static class ThreadProbeConfig {

    private final AtomicBoolean wasCalled = new AtomicBoolean(false);
    private final AtomicBoolean wasVirtual = new AtomicBoolean(false);

    @Bean
    Filter threadProbeFilter() {
      return (request, response, chain) -> {
        wasCalled.set(true);
        wasVirtual.set(Thread.currentThread().isVirtual());
        chain.doFilter(request, response);
      };
    }
  }
}
