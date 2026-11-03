package com.algoprep.common.domain.ports.out;

import java.util.UUID;

/**
 * Sinh định danh cho một Aggregate mới.
 *
 * <p>Sinh id ở tầng ứng dụng chứ không để database sinh: một Aggregate phải có định danh
 * <strong>trước khi</strong> được ghi, vì nó cần định danh đó để phát sự kiện miền và để đẩy trạng
 * thái realtime — cả hai đều xảy ra trong cùng transaction với lần ghi đầu tiên.
 */
public interface IdGeneratorPort {

  /**
   * @return một định danh mới, chưa từng được trả về trước đó
   */
  UUID newId();
}
