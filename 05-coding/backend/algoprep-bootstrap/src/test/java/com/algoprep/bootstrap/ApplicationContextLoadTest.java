package com.algoprep.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context nạp được, và các cổng ra dùng chung có adapter thật.
 *
 * <p>Bộ test rẻ nhất bắt được nhiều lỗi nhất ở khung base: sai cấu hình bean, trùng bean, sai
 * {@code scanBasePackages}, thiếu adapter cho một cổng đã khai.
 *
 * <p>Không cần Docker — có chủ đích. Ở base, {@code algoprep-bootstrap} chưa nối PostgreSQL/Redis/
 * RabbitMQ, nên {@code mvnw verify} chạy được trên một máy chưa bật hạ tầng. Test tích hợp có hạ
 * tầng thật dùng Testcontainers và thuộc slice của từng module ({@code 01-rd/system/environment.md}
 * mục 5 — KHÔNG mock database bằng H2).
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextLoadTest {

  @Autowired private ClockPort clockPort;

  @Autowired private IdGeneratorPort idGeneratorPort;

  @Test
  @DisplayName("nạp được context và lắp đủ adapter cho cổng ra dùng chung")
  void napDuocContext() {
    assertThat(clockPort).isNotNull();
    assertThat(clockPort.now()).isNotNull();
    assertThat(idGeneratorPort).isNotNull();
  }

  @Test
  @DisplayName("cổng sinh id trả về giá trị khác nhau mỗi lần gọi")
  void sinhIdKhongTrungLap() {
    assertThat(idGeneratorPort.newId()).isNotEqualTo(idGeneratorPort.newId());
  }
}
