package com.algoprep.bootstrap.config;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lắp adapter thật cho các cổng ra mà {@code algoprep-common} khai.
 *
 * <p>Nhân dùng chung khai cổng, tầng khởi chạy lắp adapter — nhờ vậy lõi nghiệp vụ của cả sáu
 * Bounded Context vẫn là Java thuần và test được tất định bằng cách truyền vào một cổng giả.
 */
@Configuration
public class CommonPortsConfig {

  /**
   * Đồng hồ hệ thống theo UTC.
   *
   * <p>UTC chứ không phải giờ máy chủ: bài nộp, phiên phỏng vấn và cửa sổ rate limit đều là dữ liệu
   * so sánh xuyên múi giờ, và một lần đổi timezone của máy chủ không được phép làm lệch chúng.
   */
  @Bean
  public ClockPort clockPort() {
    Clock utc = Clock.systemUTC();
    return new ClockPort() {
      @Override
      public Instant now() {
        return utc.instant();
      }
    };
  }

  /** Sinh định danh bằng UUID phiên bản 4. */
  @Bean
  public IdGeneratorPort idGeneratorPort() {
    return new IdGeneratorPort() {
      @Override
      public UUID newId() {
        return UUID.randomUUID();
      }
    };
  }
}
