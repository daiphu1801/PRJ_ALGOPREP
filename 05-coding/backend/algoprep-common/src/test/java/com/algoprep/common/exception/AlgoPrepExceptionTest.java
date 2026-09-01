package com.algoprep.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Mã lỗi là hợp đồng với frontend, nên nó phải luôn tồn tại.
 *
 * <p>Bộ test này nhỏ có chủ đích: ở khung base, {@code algoprep-common} chỉ có ngần này hành vi. Nó
 * tồn tại để chứng minh JUnit 5 + AssertJ + JaCoCo thật sự chạy trong reactor, không phải để đạt
 * một con số bao phủ.
 */
class AlgoPrepExceptionTest {

  @Test
  @DisplayName("giữ nguyên mã lỗi và message đã truyền vào")
  void giuNguyenMaLoiVaMessage() {
    var ex = new DomainRuleViolationException("SUBMISSION_LANGUAGE_UNSUPPORTED", "ngôn ngữ Rust");

    assertThat(ex.errorCode()).isEqualTo("SUBMISSION_LANGUAGE_UNSUPPORTED");
    assertThat(ex.getMessage()).isEqualTo("ngôn ngữ Rust");
    assertThat(ex).isInstanceOf(AlgoPrepException.class);
  }

  @Test
  @DisplayName("giữ nguyên nguyên nhân gốc để không mất stack trace")
  void giuNguyenNguyenNhanGoc() {
    var cause = new IllegalStateException("gốc");

    var ex = new DomainRuleViolationException("ANY_CODE", "bọc lại", cause);

    assertThat(ex).hasCause(cause);
  }

  @Test
  @DisplayName("từ chối mã lỗi rỗng ngay lúc khởi tạo")
  void tuChoiMaLoiRong() {
    assertThatThrownBy(() -> new ResourceNotFoundException("   ", "không tìm thấy"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("từ chối mã lỗi null ngay lúc khởi tạo")
  void tuChoiMaLoiNull() {
    assertThatThrownBy(() -> new PermissionDeniedException(null, "không có quyền"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
