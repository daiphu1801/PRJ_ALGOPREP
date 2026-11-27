package com.algoprep.common.domain.ports.out;

import java.time.Instant;

/**
 * Đọc thời gian hiện tại.
 *
 * <p>Lý do tồn tại: {@code Instant.now()} gọi thẳng trong lõi nghiệp vụ làm cho mọi luật phụ thuộc
 * thời gian trở nên không test được tất định. Trong AlgoPrep điều đó chạm vào những chỗ đắt nhất —
 * timeout sweep của bài nộp treo ({@code F4-07}), thời hạn refresh token, cửa sổ rate limit.
 *
 * <p>Adapter hiện thực nằm ở {@code algoprep-bootstrap}, không ở đây: nhân dùng chung khai cổng,
 * tầng khởi chạy lắp adapter.
 */
public interface ClockPort {

  /**
   * @return thời điểm hiện tại theo UTC
   */
  Instant now();
}
