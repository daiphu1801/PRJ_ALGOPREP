package com.algoprep.common.domain;

import java.time.Instant;

/**
 * Dấu hiệu cho một sự kiện miền.
 *
 * <p>Sự kiện miền là <strong>một trong đúng ba cách</strong> hai module được phép nói chuyện với
 * nhau ({@code 01-rd/system/backend_architecture.md} mục 5); hai cách còn lại là cổng ra do module
 * gọi tự khai, và đường đọc riêng. Không có cách thứ tư, và import chéo module thì ArchUnit chặn.
 *
 * <p>Sự kiện miền là <strong>bất đồng bộ có chủ ý</strong>: nếu ghép việc cập nhật tiến độ cá nhân
 * vào transaction ghi nhận kết quả chấm thì một lỗi ở {@code identity} sẽ rollback kết quả chấm bài
 * (cùng mục 5). Đó là suy giảm có kiểm soát ở mức mã nguồn.
 *
 * <p>Tên chính thức và payload của từng sự kiện chưa chốt — thuộc {@code
 * 02-bd/architecture/<module>.md} (mục 8 của tài liệu trên).
 */
public interface DomainEvent {

  /**
   * Thời điểm việc đã xảy ra — thời điểm của nghiệp vụ, không phải thời điểm phát hay tiêu thụ
   * message.
   *
   * @return mốc thời gian, không {@code null}
   */
  Instant occurredAt();
}
